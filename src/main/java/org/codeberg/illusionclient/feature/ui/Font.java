package org.codeberg.illusionclient.feature.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;

import java.util.ArrayList;
import java.util.List;

public enum Font {
	INTER {
		private static final float SIZE = 9.5F;
		private static final float HEADING_SIZE = 11.5F;

		@Override
		void draw(String text, float x, float y, int color, Draw.Weight weight) {
			weight.font.draw(text, x, y, SIZE, color);
		}

		@Override
		void heading(String text, float x, float y, int color) {
			FontRenderer.INTER_SEMIBOLD.draw(text, x, y, HEADING_SIZE, color);
		}

		@Override
		int width(String text, Draw.Weight weight) {
			return (int) Math.ceil(weight.font.width(text, SIZE));
		}

		@Override
		String trim(String text, int width, Draw.Weight weight) {
			return weight.font.trim(text, width, SIZE);
		}

		@Override
		float height() {
			return FontRenderer.INTER_MEDIUM.capHeight(SIZE);
		}

		@Override
		float headingHeight() {
			return FontRenderer.INTER_SEMIBOLD.capHeight(HEADING_SIZE);
		}

		@Override
		int lineHeight() {
			return 13;
		}
	},
	MINECRAFT {
		@Override
		void draw(String text, float x, float y, int color, Draw.Weight weight) {
			Draw.vanillaText(text, x, y, color);
		}

		@Override
		void heading(String text, float x, float y, int color) {
			Draw.vanillaText(text, x, y, color);
		}

		@Override
		int width(String text, Draw.Weight weight) {
			return renderer().getWidth(text);
		}

		@Override
		String trim(String text, int width, Draw.Weight weight) {
			return renderer().trim(text, width);
		}

		@Override
		List<String> split(String text, int width, Draw.Weight weight) {
			return renderer().split(text, width);
		}

		@Override
		float height() {
			return 8.0F;
		}

		@Override
		float headingHeight() {
			return 8.0F;
		}

		@Override
		int lineHeight() {
			return 10;
		}

		private static TextRenderer renderer() {
			return Minecraft.getInstance().textRenderer;
		}
	};

	abstract void draw(String text, float x, float y, int color, Draw.Weight weight);

	abstract void heading(String text, float x, float y, int color);

	abstract int width(String text, Draw.Weight weight);

	abstract String trim(String text, int width, Draw.Weight weight);

	abstract float height();

	abstract float headingHeight();

	abstract int lineHeight();

	List<String> split(String text, int width, Draw.Weight weight) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			if (!line.isEmpty() && width(line + " " + word, weight) > width) {
				lines.add(line.toString());
				line.setLength(0);
			}
			if (!line.isEmpty()) line.append(' ');
			line.append(word);
		}
		lines.add(line.toString());
		return lines;
	}
}
