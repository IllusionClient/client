package org.codeberg.illusionclient.feature.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Window;
import net.minecraft.client.render.platform.GlStateManager;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.module.client.ClickGuiModule;
import org.lwjgl.input.Mouse;

public abstract class ClickScreen extends Screen {
	private static final int MIN_WIDTH = 320, MIN_HEIGHT = 240;

	protected int scale;

	private static int scale(Minecraft minecraft) {
		int wanted = IllusionClient.MODULES.get(ClickGuiModule.class).scale();
		if (wanted == 0) return new Window(minecraft).getScale();

		int scale = 1;
		while (scale < wanted && minecraft.width / (scale + 1) >= MIN_WIDTH && minecraft.height / (scale + 1) >= MIN_HEIGHT) scale++;
		return scale;
	}

	@Override
	public void init(Minecraft minecraft, int width, int height) {
		scale = scale(minecraft);
		super.init(minecraft, Math.ceilDiv(minecraft.width, scale), Math.ceilDiv(minecraft.height, scale));
	}

	@Override
	public final void render(int mouseX, int mouseY, float tickDelta) {
		if (scale(minecraft) != scale) init(minecraft, 0, 0);

		float factor = scale / (float) new Window(minecraft).getScale();
		GlStateManager.pushMatrix();
		GlStateManager.scalef(factor, factor, 1.0F);
		Draw.setPixelScale(scale);

		renderScaled(Mouse.getX() * width / minecraft.width, height - Mouse.getY() * height / minecraft.height - 1, tickDelta);

		Draw.flush();
		Draw.setPixelScale(0);
		GlStateManager.popMatrix();
	}

	protected abstract void renderScaled(int mouseX, int mouseY, float tickDelta);
}
