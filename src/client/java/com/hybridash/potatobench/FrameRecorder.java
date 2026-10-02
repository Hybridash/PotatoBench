package com.hybridash.potatobench;

import java.util.Arrays;

/** Stores frame times (nanoseconds) in a growable array without boxing, so recording doesn't slow the game down. */
public class FrameRecorder {
	private long[] frames;
	private int size;

	public FrameRecorder(int initialCapacity) {
		frames = new long[Math.max(64, initialCapacity)];
	}

	public void add(long nanos) {
		if (size == frames.length) frames = Arrays.copyOf(frames, frames.length * 2);
		frames[size++] = nanos;
	}

	public int size() {
		return size;
	}

	/** Frame times sorted slowest first. */
	public long[] sortedSlowestFirst() {
		long[] copy = Arrays.copyOf(frames, size);
		Arrays.sort(copy);
		for (int i = 0, j = copy.length - 1; i < j; i++, j--) {
			long t = copy[i];
			copy[i] = copy[j];
			copy[j] = t;
		}
		return copy;
	}

	/**
	 * "X% low" FPS: the average FPS of the slowest X% of frames.
	 * This is what shows stutter. A game can average 200 FPS and still feel bad if its 1% low is 30.
	 */
	public static double lowFps(long[] slowestFirst, double percent) {
		if (slowestFirst.length == 0) return 0;
		int n = Math.max(1, (int) Math.round(slowestFirst.length * percent / 100.0));
		long sum = 0;
		for (int i = 0; i < n; i++) sum += slowestFirst[i];
		double avgNanos = sum / (double) n;
		return 1e9 / avgNanos;
	}
}
