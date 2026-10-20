package org.codeberg.illusionclient.feature.ui.component;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.List;

public abstract class Group extends Component {
	protected final List<Component> children = new ObjectArrayList<>();
	private final List<Component> visible = new ObjectArrayList<>();
	protected boolean open;

	protected abstract void renderHeader(int mouseX, int mouseY);

	protected abstract void headerClicked(int mouseX, int mouseY, int button);

	private List<Component> visibleChildren() {
		visible.clear();
		for (Component child : children) {
			if (child.visible()) visible.add(child);
		}
		return visible;
	}

	protected final float headerRadius() {
		return bottom && (!open || children.isEmpty()) ? RADIUS : 0.0F;
	}

	@Override
	public int height() {
		int height = HEIGHT;
		if (open) {
			for (Component child : children) {
				if (child.visible()) height += child.height();
			}
		}
		return height;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		renderHeader(mouseX, mouseY);
		if (!open) return;

		List<Component> visible = visibleChildren();
		int childY = y + HEIGHT;
		for (Component child : visible) {
			child.place(x, childY, width, bottom && child == visible.getLast());
			child.render(mouseX, mouseY);
			childY += child.height();
		}
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (hovered(mouseX, mouseY)) {
			headerClicked(mouseX, mouseY, button);
		} else if (open) {
			for (Component child : visibleChildren()) child.mouseClicked(mouseX, mouseY, button);
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
			if (child.visible() && child.keyPressed(key)) return true;
		}
		return false;
	}
}
