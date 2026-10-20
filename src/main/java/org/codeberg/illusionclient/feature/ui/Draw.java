package org.codeberg.illusionclient.feature.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.resource.Identifier;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.client.ClickGuiModule;
import org.lwjgl.opengl.GL11;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.awt.*;
import java.util.List;

public final class Draw {
	public static final int PANEL_SHADOW = 0x40000000;
	public static final int HUD_BACKGROUND = 0xA0181818;
	public static int SURFACE, SURFACE_LOW, CONTAINER, CONTAINER_HIGH, CONTAINER_HIGHEST;
	public static int PRIMARY, PRIMARY_CONTAINER, PRIMARY_CONTAINER_HOVER, ON_PRIMARY_CONTAINER;
	public static int SECONDARY_CONTAINER, ON_SECONDARY_CONTAINER;
	public static int TERTIARY_CONTAINER, ON_TERTIARY_CONTAINER;
	public static int ON_ACCENT, TOOLTIP_INK, SCRIM;
	public static int DIVIDER, TRACK, TEXT, MUTED, FAINT;

	private static final Identifier FONT = new Identifier("textures/font/ascii.png");
	private static final BufferUploader UPLOADER = new BufferUploader();
	private static final BufferBuilder SHAPES = new BufferBuilder(65536);
	private static final BufferBuilder GLYPHS = new BufferBuilder(65536);
	private static final int ARC_SEGMENTS = 6;
	private static final int ARC_POINTS = ARC_SEGMENTS + 1;
	private static final int OUTLINE_POINTS = ARC_POINTS * 4;
	private static final int SHADOW_STEPS = 8;
	private static final float[] COS = new float[OUTLINE_POINTS];
	private static final float[] SIN = new float[OUTLINE_POINTS];
	private static final float[] EDGE_X = new float[OUTLINE_POINTS];
	private static final float[] EDGE_Y = new float[OUTLINE_POINTS];
	private static final float[] FRINGE_X = new float[OUTLINE_POINTS];
	private static final float[] FRINGE_Y = new float[OUTLINE_POINTS];
	private static final Object2ObjectMap<Identifier, BufferBuilder> TEXTURED = new Object2ObjectOpenHashMap<>();
	private static final List<Identifier> TEXTURES_PENDING = new ObjectArrayList<>();
	private static boolean shapesPending, glyphsPending;
	private static int pixelScale;
	private static int tintedFrom;

	static {
		tint(0xFF29E38F);
		for (int i = 0; i < OUTLINE_POINTS; i++) {
			double angle = (i / ARC_POINTS + (i % ARC_POINTS) / (double) ARC_SEGMENTS) * Math.PI / 2.0D;
			COS[i] = (float) Math.cos(angle);
			SIN[i] = (float) Math.sin(angle);
		}
	}

	private Draw() {
	}

	public static Color accent() {
		Color accent = IllusionClient.MODULES.get(ClickGuiModule.class).color;
		if (accent.getRGB() != tintedFrom) tint(accent.getRGB());
		return accent;
	}

	private static void tint(int accent) {
		tintedFrom = accent;
		double[] seed = Tones.lch(accent);
		double hue = seed[2];
		double tertiary = (hue + 60.0D) % 360.0D;
		double scale = Math.min(1.0D, seed[1] / 40.0D);
		double neutral = 10.0D * scale, variant = 15.0D * scale;
		double secondary = 24.0D * scale, third = 36.0D * scale;
		double primary = Math.min(seed[1], 64.0D);

		SURFACE = Tones.of(hue, neutral, 6, 0xFF);
		SURFACE_LOW = Tones.of(hue, neutral, 10, 0xFF);
		CONTAINER = Tones.of(hue, neutral, 12, 0xFF);
		CONTAINER_HIGH = Tones.of(hue, neutral, 17, 0xFF);
		CONTAINER_HIGHEST = Tones.of(hue, neutral, 22, 0xFF);

		PRIMARY = Tones.of(hue, primary, 80, 0xFF);
		PRIMARY_CONTAINER = Tones.of(hue, primary, 30, 0xFF);
		PRIMARY_CONTAINER_HOVER = Tones.of(hue, primary, 35, 0xFF);
		ON_PRIMARY_CONTAINER = Tones.of(hue, Math.min(primary, 40.0D), 90, 0xFF);
		SECONDARY_CONTAINER = Tones.of(hue, secondary, 30, 0xFF);
		ON_SECONDARY_CONTAINER = Tones.of(hue, secondary, 90, 0xFF);
		TERTIARY_CONTAINER = Tones.of(tertiary, third, 30, 0xFF);
		ON_TERTIARY_CONTAINER = Tones.of(tertiary, third, 90, 0xFF);
		ON_ACCENT = Tones.of(hue, primary, seed[0] > 60.0D ? 15 : 95, 0xFF);

		TOOLTIP_INK = Tones.of(hue, neutral, 4, 0xF2);
		SCRIM = Tones.of(hue, neutral, 2, 0x80);
		DIVIDER = Tones.of(hue, variant, 30, 0x90);
		TRACK = SECONDARY_CONTAINER;
		TEXT = Tones.of(hue, neutral, 90, 0xFF);
		MUTED = Tones.of(hue, variant, 80, 0xFF);
		FAINT = Tones.of(hue, variant, 60, 0xFF);
	}

	public static int color(Description.Type type) {
		return type == Description.Type.INFO ? MUTED : type.color;
	}

	public static void flush() {
		if (shapesPending) {
			shapesPending = false;

			GlStateManager.disableTexture();
			GlStateManager.enableBlend();
			GlStateManager.disableAlphaTest();
			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
			GlStateManager.shadeModel(GL11.GL_SMOOTH);

			SHAPES.end();
			UPLOADER.end(SHAPES);

			GlStateManager.shadeModel(GL11.GL_FLAT);
			GlStateManager.disableBlend();
			GlStateManager.enableAlphaTest();
			GlStateManager.enableTexture();
		}

		if (!TEXTURES_PENDING.isEmpty()) {
			GlStateManager.enableBlend();
			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
			for (Identifier texture : TEXTURES_PENDING) {
				BufferBuilder buffer = TEXTURED.get(texture);
				Minecraft.getInstance().getTextureManager().bind(texture);
				buffer.end();
				UPLOADER.end(buffer);
			}
			TEXTURES_PENDING.clear();
			GlStateManager.disableBlend();
		}

		if (glyphsPending) {
			glyphsPending = false;

			Minecraft.getInstance().getTextureManager().bind(FONT);
			GLYPHS.end();
			UPLOADER.end(GLYPHS);
		}

		FontRenderer.INTER_MEDIUM.flush();
		FontRenderer.INTER_REGULAR.flush();
		FontRenderer.INTER_SEMIBOLD.flush();
	}

	public static void rect(int x, int y, int width, int height, int color) {
		fill(x, y, x + width, y + height, color);
	}

	public static void rounded(int x, int y, int width, int height, float radius, int color) {
		rounded(x, y, width, height, radius, radius, color);
	}

	public static void rounded(int x, int y, int width, int height, float top, float bottom, int color) {
		float limit = Math.min(width, height) / 2.0F;
		top = Math.min(top, limit);
		bottom = Math.min(bottom, limit);
		if (top <= 0.0F && bottom <= 0.0F) {
			fill(x, y, x + width, y + height, color);
			return;
		}

		float feather = feather();
		arc(0, x + width - top, y + top, top, feather);
		arc(1, x + top, y + top, top, feather);
		arc(2, x + bottom, y + height - bottom, bottom, feather);
		arc(3, x + width - bottom, y + height - bottom, bottom, feather);

		fan(shapes(), x + width / 2.0F, y + height / 2.0F, 0, OUTLINE_POINTS, color);
	}

	public static void shadow(int x, int y, int width, int height, float radius, float spread, int color) {
		radius = Math.min(radius, Math.min(width, height) / 2.0F);
		float left = x + radius, right = x + width - radius;
		float top = y + radius, bottom = y + height - radius;
		arc(0, right, top, radius, 0.0F);
		arc(1, left, top, radius, 0.0F);
		arc(2, left, bottom, radius, 0.0F);
		arc(3, right, bottom, radius, 0.0F);
		fan(shapes(), x + width / 2.0F, y + height / 2.0F, 0, OUTLINE_POINTS, color);

		int alpha = color >>> 24, rgb = color & 0xFFFFFF;
		for (int i = 0; i < SHADOW_STEPS; i++) {
			float from = i / (float) SHADOW_STEPS, to = (i + 1) / (float) SHADOW_STEPS;
			ring(left, right, top, bottom, radius + spread * from, radius + spread * to,
				fade(alpha, from) << 24 | rgb, fade(alpha, to) << 24 | rgb);
		}
	}

	private static int fade(int alpha, float t) {
		return Math.round(alpha * (1.0F - t * t * (3.0F - 2.0F * t)));
	}

	public static void roundedOutline(int x, int y, int width, int height, float radius, float thickness, int color) {
		radius = Math.min(radius, Math.min(width, height) / 2.0F);
		if (radius <= 0.0F) {
			fill(x - thickness, y - thickness, x + width + thickness, y, color);
			fill(x - thickness, y + height, x + width + thickness, y + height + thickness, color);
			fill(x - thickness, y, x, y + height, color);
			fill(x + width, y, x + width + thickness, y + height, color);
			return;
		}

		float left = x + radius, right = x + width - radius;
		float top = y + radius, bottom = y + height - radius;
		float outer = radius + thickness;
		float feather = feather();
		int clear = color & 0xFFFFFF;

		if (thickness >= feather * 2.0F) {
			ring(left, right, top, bottom, Math.max(radius - feather, 0.0F), radius + feather, clear, color);
			ring(left, right, top, bottom, radius + feather, outer - feather, color, color);
			ring(left, right, top, bottom, outer - feather, outer + feather, color, clear);
		} else {
			float middle = radius + thickness / 2.0F;
			int faded = Math.round((color >>> 24) * thickness / (feather * 2.0F)) << 24 | clear;
			ring(left, right, top, bottom, Math.max(radius - feather, 0.0F), middle, clear, faded);
			ring(left, right, top, bottom, middle, outer + feather, faded, clear);
		}
	}

	private static void ring(float left, float right, float top, float bottom, float inner, float outer, int innerColor, int outerColor) {
		if (outer <= inner) return;

		BufferBuilder buffer = shapes();
		for (int from = 0; from < OUTLINE_POINTS; from++) {
			int to = (from + 1) % OUTLINE_POINTS;
			float fromX = from / ARC_POINTS == 1 || from / ARC_POINTS == 2 ? left : right;
			float fromY = from / ARC_POINTS < 2 ? top : bottom;
			float toX = to / ARC_POINTS == 1 || to / ARC_POINTS == 2 ? left : right;
			float toY = to / ARC_POINTS < 2 ? top : bottom;

			vertex(buffer, fromX + COS[from] * inner, fromY - SIN[from] * inner, innerColor);
			vertex(buffer, fromX + COS[from] * outer, fromY - SIN[from] * outer, outerColor);
			vertex(buffer, toX + COS[to] * outer, toY - SIN[to] * outer, outerColor);

			vertex(buffer, fromX + COS[from] * inner, fromY - SIN[from] * inner, innerColor);
			vertex(buffer, toX + COS[to] * outer, toY - SIN[to] * outer, outerColor);
			vertex(buffer, toX + COS[to] * inner, toY - SIN[to] * inner, innerColor);
		}
	}

	public static void corner(float cornerX, float cornerY, float radius, int quadrant, int color) {
		float centerX = quadrant == 1 || quadrant == 2 ? cornerX + radius : cornerX - radius;
		float centerY = quadrant < 2 ? cornerY + radius : cornerY - radius;
		float feather = Math.min(feather(), radius);
		float outer = radius + feather, inner = radius - feather;
		int clear = color & 0xFFFFFF;

		BufferBuilder buffer = shapes();
		for (int i = 0; i < ARC_SEGMENTS; i++) {
			int from = quadrant * ARC_POINTS + i, to = from + 1;
			float fromOuterX = centerX + COS[from] * outer, fromOuterY = centerY - SIN[from] * outer;
			float toOuterX = centerX + COS[to] * outer, toOuterY = centerY - SIN[to] * outer;
			float fromInnerX = centerX + COS[from] * inner, fromInnerY = centerY - SIN[from] * inner;
			float toInnerX = centerX + COS[to] * inner, toInnerY = centerY - SIN[to] * inner;

			vertex(buffer, cornerX, cornerY, color);
			vertex(buffer, toOuterX, toOuterY, color);
			vertex(buffer, fromOuterX, fromOuterY, color);

			vertex(buffer, fromOuterX, fromOuterY, color);
			vertex(buffer, toInnerX, toInnerY, clear);
			vertex(buffer, fromInnerX, fromInnerY, clear);

			vertex(buffer, fromOuterX, fromOuterY, color);
			vertex(buffer, toOuterX, toOuterY, color);
			vertex(buffer, toInnerX, toInnerY, clear);
		}
	}

	public static void fill(float left, float top, float right, float bottom, int color) {
		fill(left, top, right, bottom, color, color);
	}

	public static void fill(float left, float top, float right, float bottom, int topColor, int bottomColor) {
		quad(shapes(), left, top, right, bottom, topColor, topColor, bottomColor, bottomColor);
	}

	public static void gradient(int x, int y, int width, int height, int topLeft, int topRight, int bottomLeft, int bottomRight) {
		quad(shapes(), x, y, x + width, y + height, topLeft, topRight, bottomLeft, bottomRight);
	}

	public static void roundedGradient(int x, int y, int width, int height, float topRadius, float bottomRadius, int topLeft, int topRight, int bottomLeft, int bottomRight) {
		float inset = Math.max(topRadius, bottomRadius);
		float left = x + inset;
		float right = x + width - inset;
		float top = y + topRadius;
		float bottom = y + height - bottomRadius;

		BufferBuilder buffer = shapes();
		quad(buffer, left, top, right, bottom, topLeft, topRight, bottomLeft, bottomRight);
		quad(buffer, left, y, right, top, topLeft, topRight, topLeft, topRight);
		quad(buffer, left, bottom, right, y + height, bottomLeft, bottomRight, bottomLeft, bottomRight);
		quad(buffer, x, top, left, bottom, topLeft, topLeft, bottomLeft, bottomLeft);
		quad(buffer, right, top, x + width, bottom, topRight, topRight, bottomRight, bottomRight);

		float feather = feather();
		arc(0, right, top, topRadius, feather);
		arc(1, left, top, topRadius, feather);
		arc(2, left, bottom, bottomRadius, feather);
		arc(3, right, bottom, bottomRadius, feather);

		fan(buffer, right, top, 0, ARC_SEGMENTS, topRight);
		fan(buffer, left, top, ARC_POINTS, ARC_SEGMENTS, topLeft);
		fan(buffer, left, bottom, ARC_POINTS * 2, ARC_SEGMENTS, bottomLeft);
		fan(buffer, right, bottom, ARC_POINTS * 3, ARC_SEGMENTS, bottomRight);
	}

	public static void texture(Identifier texture, int x, int y, int width, int height, int color) {
		BufferBuilder buffer = TEXTURED.computeIfAbsent(texture, _ -> new BufferBuilder(4096));
		boolean pending = TEXTURES_PENDING.contains(texture);
		if (pending && full(buffer, 4)) {
			flush();
			pending = false;
		}
		if (!pending) {
			TEXTURES_PENDING.add(texture);
			buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		}

		textured(buffer, x, y, 0.0F, 0.0F, color);
		textured(buffer, x, y + height, 0.0F, 1.0F, color);
		textured(buffer, x + width, y + height, 1.0F, 1.0F, color);
		textured(buffer, x + width, y, 1.0F, 0.0F, color);
	}

	public static void text(String text, float x, float y, int color) {
		text(text, x, y, color, Weight.MEDIUM);
	}

	public static void text(String text, float x, float y, int color, Weight weight) {
		font().draw(text, x, y, opaque(color), weight);
	}

	static void vanillaText(String text, float x, float y, int color) {
		TextRenderer renderer = Minecraft.getInstance().textRenderer;
		if (!batchable(renderer, text)) {
			flush();
			renderer.drawWithShadow(text, x, y, color);
			return;
		}

		glyphs(renderer, text, x + 1.0F, y + 1.0F, (color & 0xFCFCFC) >> 2 | color & 0xFF000000);
		glyphs(renderer, text, x, y, color);
	}

	public static void heading(String text, float x, float y, int color) {
		font().heading(text, x, y, opaque(color));
	}

	private static int opaque(int color) {
		return (color & 0xFC000000) == 0 ? color | 0xFF000000 : color;
	}

	public static void textRight(String text, float right, float y, int color) {
		textRight(text, right, y, color, Weight.MEDIUM);
	}

	public static void textRight(String text, float right, float y, int color, Weight weight) {
		text(text, right - width(text, weight), y, color, weight);
	}

	public static int width(String text) {
		return width(text, Weight.MEDIUM);
	}

	public static int width(String text, Weight weight) {
		return font().width(text, weight);
	}

	public static String trim(String text, int width) {
		return trim(text, width, Weight.MEDIUM);
	}

	public static String trim(String text, int width, Weight weight) {
		return font().trim(text, width, weight);
	}

	public static List<String> split(String text, int width, Weight weight) {
		return font().split(text, width, weight);
	}

	public static float textHeight() {
		return font().height();
	}

	public static int lineHeight() {
		return font().lineHeight();
	}

	public static float textY(float top, float height) {
		return top + (height - font().height()) / 2.0F;
	}

	public static float headingY(float top, float height) {
		return top + (height - font().headingHeight()) / 2.0F;
	}

	public static float headingBaseline(float headingY) {
		return headingY + font().headingHeight() - font().height();
	}

	public static Font font() {
		return IllusionClient.MODULES.get(ClickGuiModule.class).font.equals("Inter") ? Font.INTER : Font.MINECRAFT;
	}

	public static boolean smooth() {
		return font() == Font.INTER;
	}

	static boolean full(BufferBuilder buffer, int vertices) {
		return (buffer.getVertexCount() + vertices) * buffer.getFormat().getVertexSize() > buffer.getBuffer().capacity();
	}

	private static BufferBuilder shapes() {
		if (shapesPending && full(SHAPES, 1024)) flush();
		if (!shapesPending) {
			shapesPending = true;
			SHAPES.begin(GL11.GL_TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
		}
		return SHAPES;
	}

	private static boolean batchable(TextRenderer renderer, String text) {
		if (renderer.getUnicode()) return false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c < ' ' || c > '~') return false;
		}
		return true;
	}

	private static void glyphs(TextRenderer renderer, String text, float x, float y, int color) {
		if (glyphsPending && full(GLYPHS, text.length() * 4)) flush();
		if (!glyphsPending) {
			glyphsPending = true;
			GLYPHS.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		}

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			int advance = renderer.getWidth(c);

			if (c != ' ') {
				float u = c % 16 * 8;
				float v = c / 16 * 8;
				float width = advance - 1.01F;

				glyph(x, y, u, v, color);
				glyph(x, y + 7.99F, u, v + 7.99F, color);
				glyph(x + width, y + 7.99F, u + width, v + 7.99F, color);
				glyph(x + width, y, u + width, v, color);
			}
			x += advance;
		}
	}

	private static void textured(BufferBuilder buffer, float x, float y, float u, float v, int color) {
		buffer.vertex(x, y, 0.0D)
			.texture(u, v)
			.color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
			.nextVertex();
	}

	private static void glyph(float x, float y, float u, float v, int color) {
		GLYPHS.vertex(x, y, 0.0D)
			.texture(u / 128.0F, v / 128.0F)
			.color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
			.nextVertex();
	}

	private static void quad(BufferBuilder buffer, float left, float top, float right, float bottom, int topLeft, int topRight, int bottomLeft, int bottomRight) {
		vertex(buffer, right, top, topRight);
		vertex(buffer, left, top, topLeft);
		vertex(buffer, left, bottom, bottomLeft);

		vertex(buffer, right, top, topRight);
		vertex(buffer, left, bottom, bottomLeft);
		vertex(buffer, right, bottom, bottomRight);
	}

	public static int pixelScale() {
		return pixelScale != 0 ? pixelScale : new Window(Minecraft.getInstance()).getScale();
	}

	static void setPixelScale(int scale) {
		pixelScale = scale;
	}

	private static float feather() {
		return 0.5F / pixelScale();
	}

	private static void arc(int quadrant, float centerX, float centerY, float radius, float feather) {
		for (int i = 0; i < ARC_POINTS; i++) {
			int point = quadrant * ARC_POINTS + i;
			float spread = i == 0 || i == ARC_SEGMENTS ? 0.0F : Math.min(feather, radius);

			EDGE_X[point] = centerX + COS[point] * (radius - spread);
			EDGE_Y[point] = centerY - SIN[point] * (radius - spread);
			FRINGE_X[point] = centerX + COS[point] * (radius + spread);
			FRINGE_Y[point] = centerY - SIN[point] * (radius + spread);
		}
	}

	private static void fan(BufferBuilder buffer, float centerX, float centerY, int first, int segments, int color) {
		int clear = color & 0xFFFFFF;

		for (int i = 0; i < segments; i++) {
			int from = (first + i) % OUTLINE_POINTS;
			int to = (first + i + 1) % OUTLINE_POINTS;

			vertex(buffer, centerX, centerY, color);
			vertex(buffer, EDGE_X[from], EDGE_Y[from], color);
			vertex(buffer, EDGE_X[to], EDGE_Y[to], color);

			if (EDGE_X[from] == FRINGE_X[from] && EDGE_Y[from] == FRINGE_Y[from]
				&& EDGE_X[to] == FRINGE_X[to] && EDGE_Y[to] == FRINGE_Y[to]) continue;

			vertex(buffer, EDGE_X[to], EDGE_Y[to], color);
			vertex(buffer, EDGE_X[from], EDGE_Y[from], color);
			vertex(buffer, FRINGE_X[from], FRINGE_Y[from], clear);

			vertex(buffer, EDGE_X[to], EDGE_Y[to], color);
			vertex(buffer, FRINGE_X[from], FRINGE_Y[from], clear);
			vertex(buffer, FRINGE_X[to], FRINGE_Y[to], clear);
		}
	}

	private static void vertex(BufferBuilder buffer, double x, double y, int color) {
		buffer.vertex(x, y, 0.0D)
			.color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
			.nextVertex();
	}

	public enum Weight {
		MEDIUM(FontRenderer.INTER_MEDIUM),
		REGULAR(FontRenderer.INTER_REGULAR);

		final FontRenderer font;

		Weight(FontRenderer font) {
			this.font = font;
		}
	}
}
