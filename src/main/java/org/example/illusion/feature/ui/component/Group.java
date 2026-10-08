package org.example.illusion.feature.ui.component;

import java.util.ArrayList;
import java.util.List;

public abstract class Group extends Component {
	protected final List<Component> children = new ArrayList<>();
	protected boolean open;

	protected abstract void renderHeader(int mouseX, int mouseY);

	protected abstract void headerClicked(int mouseX, int mouseY, int button);

	protected final float headerRadius() {
		return bottom && (!open || children.isEmpty()) ? RADIUS : 0.0F;
	}

	@Override
	public int height() {
		int height = HEIGHT;
		if (open) {
			for (Component child : children) height += child.height();
		}
		return height;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		renderHeader(mouseX, mouseY);
		if (!open) return;

		int childY = y + HEIGHT;
		for (Component child : children) {
			child.x = x;
			child.y = childY;
			child.bottom = bottom && child == children.get(children.size() - 1);
			child.render(mouseX, mouseY);
			childY += child.height();
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (hovered(mouseX, mouseY)) {
			headerClicked(mouseX, mouseY, button);
		} else if (open) {
			for (Component child : children) child.mouseClicked(mouseX, mouseY, button);
		}
	}

	@Override
	public void mouseReleased() {
		for (Component child : children) child.mouseReleased();
	}

	@Override
	public boolean keyPressed(int key) {
		if (!open) return false;

		for (Component child : children) {
			if (child.keyPressed(key)) return true;
		}
		return false;
	}
}
