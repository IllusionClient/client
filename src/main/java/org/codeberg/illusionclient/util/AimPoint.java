package org.codeberg.illusionclient.util;

import java.util.Random;

public final class AimPoint {
	private static final double EDGE_INSET = 0.05D;
	private final Random random = new Random();
	private final float[] noise = new float[3];
	private final float[] noiseGoal = new float[3];
	private float wait;

	public static double axis(double eye, double min, double max, float multipoint, float spread, float noise) {
		double size = max - min;
		double center = (min + max) / 2.0D;
		double inset = Math.min(EDGE_INSET, size / 2.0D);
		double reach = Math.min(size * spread, size / 2.0D - inset);
		double closest = clamp(eye, min + inset + reach, max - inset - reach);
		double point = center + (closest - center) * multipoint / 100.0F + reach * noise;
		return clamp(point, min + inset, max - inset);
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}

	public void advance(float delta) {
		wait -= delta;
		if (wait <= 0.0F) {
			wait = 0.2F + random.nextInt(300) / 1000.0F;
			for (int i = 0; i < 3; i++) noiseGoal[i] = random.nextFloat() * 2.0F - 1.0F;
		}
		float blend = 1.0F - (float) Math.exp(-4.0F * delta);
		for (int i = 0; i < 3; i++) noise[i] += (noiseGoal[i] - noise[i]) * blend;
	}

	public float noise(int axis) {
		return noise[axis];
	}
}
