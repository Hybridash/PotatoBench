package com.hybridash.potatobench;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/** Saves every run to .minecraft/potatobench/results.csv (opens in Excel / Google Sheets). */
public final class Results {
	private static final Path FILE = FabricLoader.getInstance().getGameDir().resolve("potatobench").resolve("results.csv");

	private Results() {}

	public static void append(BenchResult r) {
		try {
			Files.createDirectories(FILE.getParent());
			if (!Files.exists(FILE)) Files.writeString(FILE, BenchResult.csvHeader() + System.lineSeparator());
			Files.writeString(FILE, r.csvLine() + System.lineSeparator(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			PotatoBenchClient.LOGGER.warn("Couldn't save results to {}", FILE, e);
		}
	}

	/** The last few runs, formatted for chat. */
	public static List<String> lastLines(int n) {
		List<String> out = new ArrayList<>();
		try {
			if (!Files.exists(FILE)) return out;
			List<String> all = Files.readAllLines(FILE);
			for (int i = Math.max(1, all.size() - n); i < all.size(); i++) {
				String[] c = all.get(i).split(",");
				if (c.length < 10) continue;
				out.add(String.format("%s  %s avg / %s 1%% low  (RD %s, %s, %s)", c[0], c[1], c[2], c[8], c[9], c[7]));
			}
		} catch (IOException e) {
			PotatoBenchClient.LOGGER.warn("Couldn't read {}", FILE, e);
		}
		return out;
	}
}
