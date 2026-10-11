package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.feature.ui.Tooltip;

public abstract class Component {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 24;
	public static final int PADDING = 8;
	public static final float RADIUS = 6.0F;
	protected static final float TEXT_Y = 8.0F;

	protected int x, y;
	protected boolean bottom;
	protected Description description;

	public int height() {
		return HEIGHT;
	}

	public boolean visible() {
		return true;
	}

	public abstract void render(int mouseX, int mouseY);

	public void mouseClicked(int mouseX, int mouseY, int button) {
	}

	public void mouseReleased() {
	}

	public boolean keyPressed(int key) {
		return false;
	}

	protected boolean hovered(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + HEIGHT;
	}

	protected final int left() {
		return x + PADDING * 2;
	}

	protected final int right() {
		return x + WIDTH - PADDING;
	}

	protected final void row(String label, int mouseX, int mouseY) {
		boolean hovered = hovered(mouseX, mouseY);
		if (hovered) Tooltip.show(description);

		Draw.rounded(x, y, WIDTH, height(), 0.0F, bottom ? RADIUS : 0.0F, hovered ? Draw.BACKGROUND_HOVER : Draw.BACKGROUND);
		Draw.rect(left(), y, right() - left(), 2, Draw.DIVIDER);
		Draw.text(label, left(), y + TEXT_Y, -1);
	}

	protected final void value(String text) {
		Draw.textRight(text, right(), y + TEXT_Y, Draw.MUTED);
	}
}
