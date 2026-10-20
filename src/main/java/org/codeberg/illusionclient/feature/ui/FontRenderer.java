package org.codeberg.illusionclient.feature.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.vertex.BufferBuilder;
import net.minecraft.client.render.vertex.BufferUploader;
import net.minecraft.client.render.vertex.DefaultVertexFormat;
import net.minecraft.client.resource.manager.ResourceManager;
import net.minecraft.resource.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class FontRenderer {
	public static final FontRenderer INTER_MEDIUM = new FontRenderer("inter_medium", 10, 19, 29, 38);
	public static final FontRenderer INTER_SEMIBOLD = new FontRenderer("inter_semibold", 12, 23, 35, 46);
	public static final FontRenderer INTER_REGULAR = new FontRenderer("inter_regular", 10, 19, 29, 38);

	private static final char FIRST = ' ', LAST = '~';
	private static final BufferUploader UPLOADER = new BufferUploader();

	private final Atlas[] atlases;

	private FontRenderer(String name, int... sizes) {
		atlases = new Atlas[sizes.length];
		for (int i = 0; i < sizes.length; i++) atlases[i] = new Atlas(name + "_" + sizes[i], sizes[i]);
	}

	public void draw(String text, float x, float y, float size, int color) {
		float scale = Draw.pixelScale();
		Atlas atlas = atlas(size * scale);
		float unit = atlas.unit(size, scale);
		if (atlas.pending && Draw.full(atlas.buffer, text.length() * 4)) Draw.flush();
		BufferBuilder buffer = atlas.begin();

		x = Math.round(x * scale) / scale;
		float baseline = Math.round((y + atlas.capHeight * unit) * scale) / scale;
		for (int i = 0; i < text.length(); i++) {
			Glyph glyph = atlas.glyph(text.charAt(i));
			if (glyph.visible()) {
				float left = x + glyph.left * unit, right = x + glyph.right * unit;
				float top = baseline + glyph.top * unit, bottom = baseline + glyph.bottom * unit;

				vertex(buffer, left, top, glyph.u0, glyph.v0, color);
				vertex(buffer, left, bottom, glyph.u0, glyph.v1, color);
				vertex(buffer, right, bottom, glyph.u1, glyph.v1, color);
				vertex(buffer, right, top, glyph.u1, glyph.v0, color);
			}
			x += glyph.advance * unit;
		}
	}

	public float width(String text, float size) {
		float scale = Draw.pixelScale();
		Atlas atlas = atlas(size * scale);
		float width = 0.0F;
		for (int i = 0; i < text.length(); i++) {
			width += atlas.glyph(text.charAt(i)).advance;
		}
		return width * atlas.unit(size, scale);
	}

	public String trim(String text, float width, float size) {
		float scale = Draw.pixelScale();
		Atlas atlas = atlas(size * scale);
		float unit = atlas.unit(size, scale), advance = 0.0F;
		for (int i = 0; i < text.length(); i++) {
			advance += atlas.glyph(text.charAt(i)).advance;
			if (advance * unit > width) return text.substring(0, i);
		}
		return text;
	}

	public float capHeight(float size) {
		float scale = Draw.pixelScale();
		Atlas atlas = atlas(size * scale);
		return atlas.capHeight * atlas.unit(size, scale);
	}

	public void flush() {
		for (Atlas atlas : atlases) atlas.flush();
	}

	private Atlas atlas(float pixels) {
		Atlas best = atlases[0];
		for (Atlas atlas : atlases) {
			if (Math.abs(atlas.size - pixels) < Math.abs(best.size - pixels)) best = atlas;
		}
		best.load();
		return best;
	}

	private static void vertex(BufferBuilder buffer, float x, float y, float u, float v, int color) {
		buffer.vertex(x, y, 0.0D)
			.texture(u, v)
			.color(color >> 16 & 255, color >> 8 & 255, color & 255, color >>> 24)
			.nextVertex();
	}

	private static final class Atlas {
		private final Identifier image, metrics;
		private final int size;
		private final BufferBuilder buffer = new BufferBuilder(65536);
		private final Glyph[] glyphs = new Glyph[LAST - FIRST + 1];
		private float capHeight;
		private int texture;
		private boolean loaded, pending;

		Atlas(String name, int size) {
			this.image = new Identifier("illusion", "font/" + name + ".png");
			this.metrics = new Identifier("illusion", "font/" + name + ".json");
			this.size = size;
		}

		float unit(float size, float scale) {
			return Math.round(size * scale) == this.size ? 1.0F / scale : size / this.size;
		}

		Glyph glyph(char c) {
			return glyphs[c < FIRST || c > LAST ? '?' - FIRST : c - FIRST];
		}

		BufferBuilder begin() {
			if (!pending) {
				pending = true;
				buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
			}
			return buffer;
		}

		void flush() {
			if (!pending) return;
			pending = false;

			GlStateManager.bindTexture(texture);
			GlStateManager.enableBlend();
			GlStateManager.disableAlphaTest();
			GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

			buffer.end();
			UPLOADER.end(buffer);

			GlStateManager.enableAlphaTest();
			GlStateManager.disableBlend();
		}

		void load() {
			if (loaded) return;
			loaded = true;

			ResourceManager resources = Minecraft.getInstance().getResourceManager();
			JsonObject root;
			BufferedImage coverage;
			try (Reader reader = new InputStreamReader(resources.getResource(metrics).asStream(), StandardCharsets.UTF_8);
				 InputStream stream = resources.getResource(image).asStream()) {
				root = JsonParser.parseReader(reader).getAsJsonObject();
				coverage = ImageIO.read(stream);
			} catch (IOException e) {
				throw new IllegalStateException("Missing font atlas " + image, e);
			}
			upload(coverage);

			float width = coverage.getWidth(), height = coverage.getHeight();
			for (JsonElement element : root.getAsJsonArray("glyphs")) {
				JsonObject json = element.getAsJsonObject();
				int index = json.get("unicode").getAsInt() - FIRST;
				if (index < 0 || index >= glyphs.length) continue;

				float advance = json.get("advance").getAsFloat();
				JsonObject plane = json.getAsJsonObject("planeBounds"), bounds = json.getAsJsonObject("atlasBounds");
				glyphs[index] = plane == null ? new Glyph(advance) : new Glyph(advance,
					plane.get("left").getAsFloat(), plane.get("top").getAsFloat(),
					plane.get("right").getAsFloat(), plane.get("bottom").getAsFloat(),
					bounds.get("left").getAsFloat() / width, bounds.get("top").getAsFloat() / height,
					bounds.get("right").getAsFloat() / width, bounds.get("bottom").getAsFloat() / height);
			}

			for (int i = 0; i < glyphs.length; i++) {
				if (glyphs[i] == null) throw new IllegalStateException(metrics + " is missing '" + (char) (FIRST + i) + "'");
			}
			capHeight = -glyph('H').top;
		}

		private void upload(BufferedImage coverage) {
			if (coverage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
				throw new IllegalStateException(image + " must be an 8-bit greyscale PNG");
			}
			byte[] pixels = ((DataBufferByte) coverage.getRaster().getDataBuffer()).getData();
			ByteBuffer data = BufferUtils.createByteBuffer(pixels.length * 4);
			for (byte alpha : pixels) data.put((byte) -1).put((byte) -1).put((byte) -1).put(alpha);
			data.flip();

			texture = GL11.glGenTextures();
			GlStateManager.bindTexture(texture);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
			GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
			GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, coverage.getWidth(), coverage.getHeight(), 0,
				GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, data);
		}
	}

	private record Glyph(float advance, float left, float top, float right, float bottom, float u0, float v0, float u1, float v1) {
		Glyph(float advance) {
			this(advance, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
		}

		boolean visible() {
			return right > left;
		}
	}
}
