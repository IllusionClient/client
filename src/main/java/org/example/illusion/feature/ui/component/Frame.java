package org.example.illusion.feature.ui.component;

import org.example.illusion.IllusionClient;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.ui.Draw;

public class Frame extends Group {
	private final Category category;
	private boolean dragging;
	private int dragX, dragY;

	public Frame(Category category, int x, int y) {
		this.category = category;
		this.x = x;
		this.y = y;
		this.bottom = true;

		for (Module module : IllusionClient.MODULES.modules) {
			if (module.category == category) children.add(new ModuleComponent(module));
		}
	}

	@Override
	protected void renderHeader(int mouseX, int mouseY) {
		if (dragging) {
			x = mouseX - dragX;
			y = mouseY - dragY;
		}

		Draw.rounded(x, y, WIDTH, HEIGHT, RADIUS, headerRadius(), Draw.accent().getRGB());
		Draw.text(category.name(), x + PADDING, y + TEXT_Y, -1);
		Draw.textRight(open ? "-" : "+", right(), y + TEXT_Y, -1);
	}

	@Override
	protected void headerClicked(int mouseX, int mouseY, int button) {
		if (button == 0) {
			dragging = true;
			dragX = mouseX - x;
			dragY = mouseY - y;
		} else if (button == 1) {
			open = !open;
		}
	}

	public void scroll(int mouseX, int amount) {
		if (!dragging && mouseX >= x && mouseX < x + WIDTH) y += amount;
	}

	@Override
	public void mouseReleased() {
		dragging = false;
		super.mouseReleased();
	}
}
