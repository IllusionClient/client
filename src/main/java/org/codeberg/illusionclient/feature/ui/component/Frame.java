package org.codeberg.illusionclient.feature.ui.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.feature.ui.Icon;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Frame extends Group {
	public static final int TITLE = 24;
	private static final int INSET = 6;
	private static final int GAP = 4;
	private static final float WINDOW_RADIUS = 8.0F;

	public final Category category;
	private final String title;
	private boolean dragging, placed;
	private int dragX, dragY;

	public Frame(Category category) {
		this.category = category;
		this.title = category.name().charAt(0) + category.name().substring(1).toLowerCase(Locale.ROOT);

		for (Module module : IllusionClient.MODULES.modules) {
			if (module.category == category) children.add(new ModuleComponent(module));
		}
	}

	@Override
	public int height() {
		if (!open || children.isEmpty()) return TITLE;

		int height = TITLE;
		for (Component child : children) height += child.height() + GAP;
		return height - GAP + INSET;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		if (dragging) {
			x = mouseX - dragX;
			y = mouseY - dragY;
		}

		int height = height();
		Draw.shadow(x, y + 2, width, height, WINDOW_RADIUS, 10.0F, Draw.PANEL_SHADOW);
		Draw.rounded(x, y, width, height, WINDOW_RADIUS, Draw.SURFACE);
		renderHeader(mouseX, mouseY);
		if (!open) return;

		int childY = y + TITLE;
		for (Component child : children) {
			child.place(x + INSET, childY, width - INSET * 2, true);
			child.render(mouseX, mouseY);
			childY += child.height() + GAP;
		}
	}

	@Override
	protected void renderHeader(int mouseX, int mouseY) {
		Icon icon = Icon.of(category);
		float headingY = Draw.headingY(y, TITLE);
		icon.draw(x + PADDING + 2, y + (TITLE - icon.height) / 2, Draw.accent().getRGB());
		Draw.heading(title, x + PADDING + 2 + icon.width + 7, headingY, Draw.TEXT);

		long on = children.stream().filter(child -> ((ModuleComponent) child).module.isEnabled()).count();
		Draw.textRight(on + " of " + children.size() + " on", x + width - PADDING - 2, Draw.headingBaseline(headingY), Draw.FAINT, Draw.Weight.REGULAR);
	}

	@Override
	protected boolean hovered(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + TITLE;
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

	public boolean placed() {
		return placed;
	}

	public void moveTo(int x, int y) {
		this.x = x;
		this.y = y;
		placed = true;
	}

	public void clamp(int screenWidth, int screenHeight) {
		x = Math.clamp(x, 0, Math.max(0, screenWidth - width));
		y = Math.clamp(y, 0, Math.max(0, screenHeight - TITLE));
	}

	public JsonObject save() {
		JsonArray expanded = new JsonArray();
		for (Component child : children) {
			ModuleComponent module = (ModuleComponent) child;
			if (module.open) expanded.add(module.module.key);
		}

		JsonObject json = new JsonObject();
		json.addProperty("x", x);
		json.addProperty("y", y);
		json.addProperty("open", open);
		json.add("expanded", expanded);
		return json;
	}

	public void load(JsonObject json) {
		int savedX = json.get("x").getAsInt(), savedY = json.get("y").getAsInt();
		boolean savedOpen = json.get("open").getAsBoolean();
		Set<String> expanded = new HashSet<>();
		for (JsonElement key : json.getAsJsonArray("expanded")) expanded.add(key.getAsString());

		moveTo(savedX, savedY);
		open = savedOpen;
		for (Component child : children) {
			ModuleComponent module = (ModuleComponent) child;
			module.open = expanded.contains(module.module.key);
		}
	}

	public void scroll(int mouseX, int amount) {
		if (!dragging && mouseX >= x && mouseX < x + width) y += amount;
	}

	@Override
	public void mouseReleased() {
		dragging = false;
		super.mouseReleased();
	}
}
