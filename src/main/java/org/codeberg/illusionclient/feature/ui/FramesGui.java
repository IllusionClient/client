package org.codeberg.illusionclient.feature.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.ui.component.Component;
import org.codeberg.illusionclient.feature.ui.component.Frame;
import org.codeberg.illusionclient.feature.ui.component.ModuleComponent;
import org.lwjgl.input.Mouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FramesGui extends ClickScreen {
	private static final Logger LOGGER = LoggerFactory.getLogger(FramesGui.class);
	private static final String STATE = "frames";

	private final List<Frame> frames = new ObjectArrayList<>();

	public FramesGui() {
		for (Category category : Category.values()) frames.add(new Frame(category));

		JsonElement saved = IllusionClient.CONFIG.gui.get(STATE);
		if (saved == null || !saved.isJsonObject()) return;
		for (Frame frame : frames) {
			JsonElement entry = saved.getAsJsonObject().get(frame.category.name());
			if (entry == null) continue;
			try {
				frame.load(entry.getAsJsonObject());
			} catch (RuntimeException e) {
				LOGGER.warn("Ignoring invalid saved layout for the {} frame: {}", frame.category, e.toString());
			}
		}
	}

	@Override
	public void init() {
		int x = Component.PADDING, y = Component.PADDING;
		for (Frame frame : frames) {
			if (!frame.placed()) {
				if (x > Component.PADDING && x + Component.WIDTH > width - Component.PADDING) {
					x = Component.PADDING;
					y += Frame.TITLE + Component.PADDING;
				}
				frame.moveTo(x, y);
				x += Component.WIDTH + Component.PADDING;
			}
			frame.clamp(width, height);
		}
	}

	@Override
	protected void renderScaled(int mouseX, int mouseY, float tickDelta) {
		Draw.accent();
		Draw.rect(0, 0, width, height, Draw.SCRIM);
		renderWatermark();
		Draw.flush();

		for (Frame frame : frames) {
			frame.render(mouseX, mouseY);
			Draw.flush();
		}

		Tooltip.render(mouseX, mouseY, width, height);
		Draw.flush();
	}

	private void renderWatermark() {
		int textX = width - Component.PADDING - Draw.width(IllusionClient.NAME);
		int y = height - Component.PADDING - Icon.LOGO.height;
		Icon.LOGO.draw(textX - 5 - Icon.LOGO.width, y, Draw.accent().getRGB());
		Draw.text(IllusionClient.NAME, textX, y, Draw.TEXT);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		ModuleComponent.stopListening();
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
	public void handleMouse() {
		super.handleMouse();

		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) return;

		int mouseX = Mouse.getEventX() * width / minecraft.width;
		for (Frame frame : frames) {
			frame.scroll(mouseX, Integer.signum(wheel) * Component.HEIGHT);
		}
	}

	@Override
	protected void keyPressed(char chr, int key) {
		for (Frame frame : frames) {
			if (frame.keyPressed(key)) return;
		}
		super.keyPressed(chr, key);
	}

	@Override
	public void removed() {
		ModuleComponent.stopListening();
		JsonObject state = new JsonObject();
		for (Frame frame : frames) {
			frame.mouseReleased();
			state.add(frame.category.name(), frame.save());
		}
		IllusionClient.CONFIG.gui.add(STATE, state);
		IllusionClient.CONFIG.save();
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}
}
