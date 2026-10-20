package org.codeberg.illusionclient.feature.ui;

public final class Tones {
	private static final double XN = 0.95047, YN = 1.0, ZN = 1.08883;

	private Tones() {
	}

	public static double[] lch(int rgb) {
		double r = linear((rgb >> 16 & 255) / 255.0D);
		double g = linear((rgb >> 8 & 255) / 255.0D);
		double b = linear((rgb & 255) / 255.0D);

		double x = f((0.4124564D * r + 0.3575761D * g + 0.1804375D * b) / XN);
		double y = f((0.2126729D * r + 0.7151522D * g + 0.0721750D * b) / YN);
		double z = f((0.0193339D * r + 0.1191920D * g + 0.9503041D * b) / ZN);

		double l = 116.0D * y - 16.0D;
		double a = 500.0D * (x - y);
		double bb = 200.0D * (y - z);
		double hue = Math.toDegrees(Math.atan2(bb, a));
		return new double[]{l, Math.hypot(a, bb), hue < 0 ? hue + 360.0D : hue};
	}

	public static int of(double hue, double chroma, double tone, int alpha) {
		chroma *= Math.min(1.0D, (tone + 4.0D) / 24.0D);
		double low = 0.0D, high = chroma;
		int best = rgb(hue, 0.0D, tone);
		if (inGamut(hue, chroma, tone)) return rgb(hue, chroma, tone) & 0xFFFFFF | alpha << 24;

		for (int i = 0; i < 16; i++) {
			double mid = (low + high) / 2.0D;
			if (inGamut(hue, mid, tone)) {
				best = rgb(hue, mid, tone);
				low = mid;
			} else {
				high = mid;
			}
		}
		return best & 0xFFFFFF | alpha << 24;
	}

	private static boolean inGamut(double hue, double chroma, double tone) {
		for (double channel : linearRgb(hue, chroma, tone)) {
			if (channel < -0.0001D || channel > 1.0001D) return false;
		}
		return true;
	}

	private static int rgb(double hue, double chroma, double tone) {
		double[] linear = linearRgb(hue, chroma, tone);
		int rgb = 0;
		for (double channel : linear) rgb = rgb << 8 | (int) Math.round(Math.clamp(gamma(channel), 0.0D, 1.0D) * 255.0D);
		return rgb;
	}

	private static double[] linearRgb(double hue, double chroma, double tone) {
		double radians = Math.toRadians(hue);
		double fy = (tone + 16.0D) / 116.0D;
		double fx = fy + chroma * Math.cos(radians) / 500.0D;
		double fz = fy - chroma * Math.sin(radians) / 200.0D;

		double x = inverse(fx) * XN;
		double y = inverse(fy) * YN;
		double z = inverse(fz) * ZN;
		return new double[]{
			3.2404542D * x - 1.5371385D * y - 0.4985314D * z,
			-0.9692660D * x + 1.8760108D * y + 0.0415560D * z,
			0.0556434D * x - 0.2040259D * y + 1.0572252D * z
		};
	}

	private static double f(double t) {
		return t > 216.0D / 24389.0D ? Math.cbrt(t) : (24389.0D / 27.0D * t + 16.0D) / 116.0D;
	}

	private static double inverse(double t) {
		return t * t * t > 216.0D / 24389.0D ? t * t * t : (116.0D * t - 16.0D) / (24389.0D / 27.0D);
	}

	private static double linear(double channel) {
		return channel <= 0.04045D ? channel / 12.92D : Math.pow((channel + 0.055D) / 1.055D, 2.4D);
	}

	private static double gamma(double channel) {
		return channel <= 0.0031308D ? channel * 12.92D : 1.055D * Math.pow(channel, 1.0D / 2.4D) - 0.055D;
	}
}
