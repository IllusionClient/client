package org.codeberg.illusionclient.util;

import net.minecraft.client.render.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class RenderUtils {
	private static boolean constantAlpha;

	public static boolean constantAlpha() {
		return constantAlpha;
	}

	public static void beginConstantAlpha(float alpha) {
		GlStateManager.enableBlend();
		GL14.glBlendColor(0.0F, 0.0F, 0.0F, alpha);
		constantAlpha = true;
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
	}

	public static void endConstantAlpha() {
		constantAlpha = false;
		GlStateManager.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
		GlStateManager.disableBlend();
	}

	public static int blendFactor(int factor) {
		if (!constantAlpha) return factor;

		return switch (factor) {
			case GL11.GL_SRC_ALPHA, GL11.GL_SRC_COLOR -> GL11.GL_CONSTANT_ALPHA;
			case GL11.GL_ONE_MINUS_SRC_ALPHA -> GL11.GL_ONE_MINUS_CONSTANT_ALPHA;
			default -> factor;
		};
	}
}
