package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.Option;
import org.codeberg.illusionclient.feature.ui.Draw;

import java.awt.*;

public class ColorComponent extends Component {
	private static final int BOX_WIDTH = WIDTH - PADDING * 3;
	private static final int BOX_HEIGHT = 80;
	private static final int HUE_HEIGHT = 12;
	private static final int HUE_SEGMENTS = 6;
	private static final int BOX_RADIUS = 6;
	private static final int HUE_RADIUS = HUE_HEIGHT / 2;

	private final Option option;
	private final float[] hsb;
	private boolean open, draggingBox, draggingHue;

	public ColorComponent(Option option) {
		this.option = option;
		Color color = option.get();
		this.hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
	}

	private static void cursor(int centerX, int centerY, int color) {
		Draw.rounded(centerX - 6, centerY - 6, 12, 12, 6.0F, -1);
		Draw.rounded(centerX - 4, centerY - 4, 8, 8, 4.0F, color);
	}

	private static float fraction(int offset, int size) {
		return Math.min(1.0F, Math.max(0.0F, offset / (float) size));
	}

	@Override
	public int height() {
		return open ? HEIGHT + BOX_HEIGHT + PADDING + HUE_HEIGHT + PADDING : HEIGHT;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		int boxX = left();
		int boxY = y + HEIGHT;
		int hueY = boxY + BOX_HEIGHT + PADDING;

		if (draggingBox) {
			hsb[1] = fraction(mouseX - boxX - BOX_RADIUS, BOX_WIDTH - BOX_RADIUS * 2);
			hsb[2] = 1.0F - fraction(mouseY - boxY - BOX_RADIUS, BOX_HEIGHT - BOX_RADIUS * 2);
		} else if (draggingHue) {
			hsb[0] = fraction(mouseX - boxX - HUE_RADIUS, BOX_WIDTH - HUE_RADIUS * 2);
		}
		if (draggingBox || draggingHue) option.set(new Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])));

		row(option.name, mouseX, mouseY);
		Draw.rounded(right() - 12, y + 6, 12, 12, 3.0F, option.<Color>get().getRGB());
		if (!open) return;

		int hue = Color.HSBtoRGB(hsb[0], 1.0F, 1.0F);
		Draw.roundedGradient(boxX, boxY, BOX_WIDTH, BOX_HEIGHT - BOX_RADIUS, BOX_RADIUS, 0.0F, -1, hue, -1, hue);
		Draw.roundedGradient(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, BOX_RADIUS, BOX_RADIUS, 0, 0, 0xFF000000, 0xFF000000);
		cursor(boxX + BOX_RADIUS + (int) (hsb[1] * (BOX_WIDTH - BOX_RADIUS * 2)),
			boxY + BOX_RADIUS + (int) ((1.0F - hsb[2]) * (BOX_HEIGHT - BOX_RADIUS * 2)), option.<Color>get().getRGB());

		int hueX = boxX + HUE_RADIUS;
		int hueWidth = BOX_WIDTH - HUE_RADIUS * 2;
		Draw.rounded(boxX, hueY, HUE_HEIGHT, HUE_HEIGHT, HUE_RADIUS, 0xFFFF0000);
		Draw.rounded(boxX + hueWidth, hueY, HUE_HEIGHT, HUE_HEIGHT, HUE_RADIUS, 0xFFFF0000);
		for (int i = 0; i < HUE_SEGMENTS; i++) {
			int left = hueX + hueWidth * i / HUE_SEGMENTS;
			int right = hueX + hueWidth * (i + 1) / HUE_SEGMENTS;
			int from = Color.HSBtoRGB(i / (float) HUE_SEGMENTS, 1.0F, 1.0F);
			int to = Color.HSBtoRGB((i + 1) / (float) HUE_SEGMENTS, 1.0F, 1.0F);
			Draw.gradient(left, hueY, right - left, HUE_HEIGHT, from, to, from, to);
		}
		cursor(hueX + (int) (hsb[0] * hueWidth), hueY + HUE_RADIUS, hue);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (hovered(mouseX, mouseY)) {
			open = !open;
		} else if (open && button == 0 && mouseX >= left() && mouseX < right()) {
			int offsetY = mouseY - y - HEIGHT;
			draggingBox = offsetY >= 0 && offsetY < BOX_HEIGHT;
			draggingHue = offsetY >= BOX_HEIGHT + PADDING && offsetY < BOX_HEIGHT + PADDING + HUE_HEIGHT;
		}
	}

	@Override
	public void mouseReleased() {
		draggingBox = false;
		draggingHue = false;
	}
}
