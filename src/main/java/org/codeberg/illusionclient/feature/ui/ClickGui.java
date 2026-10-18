package org.codeberg.illusionclient.feature.ui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.screen.Screen;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.ui.component.Component;
import org.codeberg.illusionclient.feature.ui.component.Frame;
import org.lwjgl.input.Mouse;

import java.util.List;

public class ClickGui extends Screen {
	private final List<Frame> frames = new ObjectArrayList<>();

	public ClickGui() {
		int x = Component.PADDING;
		for (Category category : Category.values()) {
			frames.add(new Frame(category, x, Component.PADDING));
			x += Component.WIDTH + Component.PADDING;
		}
	}

	@Override
	public void render(int mouseX, int mouseY, float tickDelta) {
		int accent = Draw.accent().getRGB() & 0xFFFFFF | 80 << 24;
		Draw.gradient(0, 0, width, height, accent, accent, 0x50000000, 0x50000000);

		for (Frame frame : frames) {
			frame.render(mouseX, mouseY);
			Draw.flush();
		}

		Tooltip.render(mouseX, mouseY, width, height);
		Draw.flush();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		for (Frame frame : frames) {
			frame.mouseClicked(mouseX, mouseY, button);
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int button) {
		for (Frame frame : frames) {
			frame.mouseReleased();
		}
	}

	@Override
	public void handleMouse() /*? if forge {*//*throws java.io.IOException*//*?}*/ {
		super.handleMouse();

		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) return;

		int mouseX = Mouse.getEventX() * width / minecraft.width;
		for (Frame frame : frames) {
			frame.scroll(mouseX, Integer.signum(wheel) * Component.HEIGHT);
		}
	}

	@Override
	protected void keyPressed(char chr, int key) /*? if forge {*//*throws java.io.IOException*//*?}*/ {
		for (Frame frame : frames) {
			if (frame.keyPressed(key)) return;
		}
		super.keyPressed(chr, key);
	}

	@Override
	public void removed() {
		for (Frame frame : frames) {
			frame.mouseReleased();
		}
		IllusionClient.CONFIG.save();
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
