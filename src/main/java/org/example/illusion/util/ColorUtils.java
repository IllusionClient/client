package org.example.illusion.util;

public final class ColorUtils {
	public static int darken(int color, float factor) {
		if (factor == 1.0F) return color;

		return color & 0xFF000000
			| (int) ((color >> 16 & 255) * factor) << 16
			| (int) ((color >> 8 & 255) * factor) << 8
			| (int) ((color & 255) * factor);
	}
}
