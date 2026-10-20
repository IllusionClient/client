package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.feature.ui.Tooltip;

public abstract class Component {
	public static final int WIDTH = 176;
	public static final int HEIGHT = 24;
	public static final int PADDING = 8;
	public static final float RADIUS = 6.0F;

	protected int x, y, width = WIDTH;
	protected boolean bottom;
	protected Description description;

	public int height() {
		return HEIGHT;
	}

	public boolean visible() {
		return true;
	}

	public final void place(int x, int y, int width, boolean bottom) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.bottom = bottom;
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
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + HEIGHT;
	}

	protected final int left() {
		return x + PADDING;
	}

	protected final int right() {
		return x + width - PADDING;
	}

	protected final void row(String label, int mouseX, int mouseY) {
		boolean hovered = hovered(mouseX, mouseY);
		if (hovered) Tooltip.show(description);

		Draw.rounded(x, y, width, height(), 0.0F, bottom ? RADIUS : 0.0F, hovered ? Draw.CONTAINER_HIGH : Draw.CONTAINER);
		Draw.rect(left(), y, right() - left(), 2, Draw.DIVIDER);
		Draw.text(label, left(), Draw.textY(y, HEIGHT), Draw.TEXT);
	}

	protected final void value(String text) {
		Draw.textRight(text, right(), Draw.textY(y, HEIGHT), Draw.MUTED);
	}
}
