package org.codeberg.illusionclient.feature.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import org.codeberg.illusionclient.feature.module.api.Description;

import java.util.List;

public final class Tooltip {
	private static final int BACKGROUND = 0xF00C0C0C;
	private static final int MAX_WIDTH = 160;
	private static final int LINE_HEIGHT = 10;
	private static final int PADDING = 6;
	private static final int OFFSET = 10;

	private static Description hovered, shown;
	private static List<String> lines;
	private static int textWidth;

	private Tooltip() {
	}

	public static void show(Description description) {
		hovered = description;
	}

	public static void render(int mouseX, int mouseY, int screenWidth, int screenHeight) {
		Description description = hovered;
		hovered = null;
		if (description == null) return;

		if (description != shown) {
			TextRenderer renderer = Minecraft.getInstance().textRenderer;
			shown = description;
			lines = renderer.split(description.value(), MAX_WIDTH);
			textWidth = 0;
			for (String line : lines) textWidth = Math.max(textWidth, renderer.getWidth(line));
		}

		int width = textWidth + PADDING * 2;
		int height = lines.size() * LINE_HEIGHT - 2 + PADDING * 2;
		int x = Math.clamp(mouseX + OFFSET, 0, Math.max(0, screenWidth - width));
		int y = mouseY + OFFSET + height > screenHeight ? Math.max(0, mouseY - OFFSET - height) : mouseY + OFFSET;

		Draw.rounded(x, y, width, height, 4.0F, BACKGROUND);
		for (int i = 0; i < lines.size(); i++) {
			Draw.text(lines.get(i), x + PADDING, y + PADDING + i * LINE_HEIGHT, description.type().color);
		}
	}
}
