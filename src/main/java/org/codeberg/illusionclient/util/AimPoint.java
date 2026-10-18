package org.codeberg.illusionclient.util;

public final class AimPoint {
	private static final double EDGE_INSET = 0.05D;

	public static double axis(double eye, double center, double min, double max, float multipoint, float spread, float noise) {
		double size = max - min;
		double inset = Math.min(EDGE_INSET, size / 2.0D);
		double reach = Math.min(size * spread, size / 2.0D - inset);
		double closest = Math.clamp(eye, min + inset + reach, max - inset - reach);
		double point = center + (closest - center) * multipoint / 100.0F + reach * noise;
		return Math.clamp(point, min + inset, max - inset);
	}
}
