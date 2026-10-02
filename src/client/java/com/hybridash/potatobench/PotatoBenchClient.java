package com.hybridash.potatobench;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * /bench measures real FPS over a fixed time while slowly spinning the camera 360°,
 * so every run renders the same amount of world and results are comparable.
 */
public class PotatoBenchClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("PotatoBench");

	private static final long WARMUP_NANOS = 3_000_000_000L;

	private enum Phase { IDLE, WARMUP, RUNNING }

	private static Phase phase = Phase.IDLE;
	private static long phaseStart;
	private static long lastFrame;
	private static int durationSeconds;
	private static boolean spin;
	private static float startYaw;
	private static FrameRecorder recorder;

	@Override
	public void onInitializeClient() {
		WorldRenderEvents.END.register(context -> onFrame());
		HudRenderCallback.EVENT.register(PotatoBenchClient::renderHud);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// Leaving the world cancels a running benchmark
			if (phase != Phase.IDLE && client.player == null) phase = Phase.IDLE;
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("bench")
						.executes(ctx -> start(ctx, 30, true))
						.then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 600))
								.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), true)))
						.then(ClientCommandManager.literal("still")
								.executes(ctx -> start(ctx, 30, false))
								.then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 600))
										.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "seconds"), false))))
						.then(ClientCommandManager.literal("stop").executes(ctx -> {
							if (phase == Phase.IDLE) {
								ctx.getSource().sendError(Text.literal("No benchmark is running."));
							} else {
								phase = Phase.IDLE;
								ctx.getSource().sendFeedback(Text.literal("Benchmark stopped.").formatted(Formatting.GRAY));
							}
							return 1;
						}))
						.then(ClientCommandManager.literal("history").executes(ctx -> {
							history(ctx.getSource());
							return 1;
						}))));
	}

	private static int start(CommandContext<FabricClientCommandSource> ctx, int seconds, boolean spinCamera) {
		if (phase != Phase.IDLE) {
			ctx.getSource().sendError(Text.literal("A benchmark is already running. /bench stop to cancel it."));
			return 0;
		}
		durationSeconds = seconds;
		spin = spinCamera;
		startYaw = ctx.getSource().getPlayer().getYaw();
		recorder = new FrameRecorder(seconds * 400);
		phase = Phase.WARMUP;
		phaseStart = System.nanoTime();
		lastFrame = 0;
		ctx.getSource().sendFeedback(Text.literal("PotatoBench: starting in 3 seconds. Don't touch the mouse"
				+ (spin ? " (the camera will spin)." : ".")).formatted(Formatting.GOLD));
		return 1;
	}

	/** Called once per rendered frame. */
	private static void onFrame() {
		if (phase == Phase.IDLE) return;
		MinecraftClient client = MinecraftClient.getInstance();
		long now = System.nanoTime();

		if (phase == Phase.WARMUP) {
			if (now - phaseStart >= WARMUP_NANOS) {
				phase = Phase.RUNNING;
				phaseStart = now;
				lastFrame = now;
			}
			return;
		}

		long frame = now - lastFrame;
		lastFrame = now;
		recorder.add(frame);

		double progress = (now - phaseStart) / (durationSeconds * 1e9);
		if (spin && client.player != null) {
			float yaw = startYaw + (float) (360.0 * Math.min(1.0, progress));
			client.player.setYaw(yaw);
			client.player.prevYaw = yaw;
		}
		if (progress >= 1.0) finish(client, (now - phaseStart) / 1e9);
	}

	private static void finish(MinecraftClient client, double elapsedSeconds) {
		phase = Phase.IDLE;
		BenchResult result = BenchResult.from(recorder, elapsedSeconds, spin, client);
		Results.append(result);

		if (client.player == null) return;
		client.player.sendMessage(Text.literal("PotatoBench results").formatted(Formatting.GOLD, Formatting.BOLD), false);
		client.player.sendMessage(Text.literal(String.format("  Average: %.0f FPS", result.avgFps())).formatted(Formatting.GREEN), false);
		client.player.sendMessage(Text.literal(String.format("  1%% low: %.0f FPS   0.1%% low: %.0f FPS", result.low1(), result.low01())).formatted(Formatting.YELLOW), false);
		client.player.sendMessage(Text.literal(String.format("  Worst frame: %.1f ms   Frames: %d", result.worstMs(), result.frames())).formatted(Formatting.GRAY), false);
		client.player.sendMessage(Text.literal("  " + result.settingsLine()).formatted(Formatting.DARK_GRAY), false);

		String summary = result.shareText();
		client.player.sendMessage(Text.literal("  [Copy results]").formatted(Formatting.AQUA, Formatting.UNDERLINE)
				.styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, summary))
						.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(summary)))), false);
	}

	private static void history(FabricClientCommandSource src) {
		List<String> lines = Results.lastLines(5);
		if (lines.isEmpty()) {
			src.sendFeedback(Text.literal("No results yet. Run /bench first.").formatted(Formatting.GRAY));
			return;
		}
		src.sendFeedback(Text.literal("Last results (newest last), full list in potatobench/results.csv:").formatted(Formatting.GOLD));
		for (String line : lines) src.sendFeedback(Text.literal("  " + line).formatted(Formatting.GRAY));
	}

	private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
		if (phase == Phase.IDLE) return;
		MinecraftClient client = MinecraftClient.getInstance();
		long now = System.nanoTime();
		String text;
		if (phase == Phase.WARMUP) {
			long left = (WARMUP_NANOS - (now - phaseStart)) / 1_000_000_000L + 1;
			text = "PotatoBench starting in " + left + "...";
		} else {
			long left = durationSeconds - (now - phaseStart) / 1_000_000_000L;
			text = "PotatoBench: " + Math.max(0, left) + "s left  ·  " + client.getCurrentFps() + " FPS";
		}
		int w = client.textRenderer.getWidth(text);
		ctx.fill(4, 4, 12 + w, 18, 0x99000000);
		ctx.drawTextWithShadow(client.textRenderer, text, 8, 7, 0xFFFFD54F);
	}
}
