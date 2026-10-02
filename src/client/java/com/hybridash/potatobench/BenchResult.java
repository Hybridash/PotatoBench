package com.hybridash.potatobench;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.opengl.GL11;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record BenchResult(
		String time,
		double avgFps,
		double low1,
		double low01,
		double worstMs,
		int frames,
		double seconds,
		boolean spin,
		int renderDistance,
		String graphics,
		String gpu,
		int mods,
		boolean sodium,
		String minecraft) {

	public static BenchResult from(FrameRecorder rec, double seconds, boolean spin, MinecraftClient client) {
		long[] slow = rec.sortedSlowestFirst();
		GameOptions o = client.options;
		String gpu;
		try {
			gpu = GL11.glGetString(GL11.GL_RENDERER);
		} catch (Throwable t) {
			gpu = "unknown";
		}
		return new BenchResult(
				LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
				rec.size() / seconds,
				FrameRecorder.lowFps(slow, 1.0),
				FrameRecorder.lowFps(slow, 0.1),
				slow.length > 0 ? slow[0] / 1e6 : 0,
				rec.size(),
				seconds,
				spin,
				o.getViewDistance().getValue(),
				o.getGraphicsMode().getValue().name().toLowerCase(),
				gpu == null ? "unknown" : gpu,
				FabricLoader.getInstance().getAllMods().size(),
				FabricLoader.getInstance().isModLoaded("sodium"),
				SharedConstants.getGameVersion().getName());
	}

	public String settingsLine() {
		return "Render distance " + renderDistance + ", " + graphics + " graphics, " + mods + " mods"
				+ (sodium ? " (Sodium)" : "") + ", " + gpu;
	}

	/** One line people can paste in Discord or a GitHub issue. */
	public String shareText() {
		return String.format("PotatoBench %s: %.0f avg / %.0f 1%% low / %.0f 0.1%% low FPS | RD %d, %s, %d mods%s | %s | MC %s",
				spin ? "spin" : "still", avgFps, low1, low01, renderDistance, graphics, mods, sodium ? " +Sodium" : "", gpu, minecraft);
	}

	public static String csvHeader() {
		return "time,avg_fps,low_1pct,low_0_1pct,worst_ms,frames,seconds,mode,render_distance,graphics,mods,sodium,minecraft,gpu";
	}

	public String csvLine() {
		return String.format(java.util.Locale.ROOT, "%s,%.1f,%.1f,%.1f,%.2f,%d,%.1f,%s,%d,%s,%d,%s,%s,\"%s\"",
				time, avgFps, low1, low01, worstMs, frames, seconds, spin ? "spin" : "still", renderDistance, graphics,
				mods, sodium, minecraft, gpu.replace("\"", "'"));
	}
}
