package org.codeberg.illusionclient.feature.ui.component;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.codeberg.illusionclient.feature.config.api.*;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.client.ClickGuiModule;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.feature.ui.Tooltip;
import org.lwjgl.input.Keyboard;

import java.util.List;

public class ModuleComponent extends Group {
	public static final int KEY_HEIGHT = 14;
	private static Module listening;

	final Module module;
	private int keyLeft, keyRight;

	public ModuleComponent(Module module) {
		this.module = module;
		this.description = module.description;
		children.addAll(options(module).stream().filter(option -> !(option instanceof KeybindComponent)).toList());
	}

	public static void stopListening() {
		listening = null;
	}

	public static void drawKey(int x, int y, String label, boolean listening, boolean bound, boolean hovered) {
		int fill, color;
		if (listening) {
			fill = Draw.PRIMARY_CONTAINER;
			color = Draw.ON_PRIMARY_CONTAINER;
		} else if (bound) {
			fill = Draw.TERTIARY_CONTAINER;
			color = Draw.ON_TERTIARY_CONTAINER;
		} else {
			fill = hovered ? Draw.CONTAINER_HIGH : Draw.CONTAINER;
			color = hovered ? Draw.TEXT : Draw.FAINT;
		}
		Draw.rounded(x, y, Draw.width(label) + 10, KEY_HEIGHT, 4.0F, fill);
		Draw.text(label, x + 5, Draw.textY(y, KEY_HEIGHT), color);
	}

	public static List<Component> options(Module module) {
		List<Component> components = new ObjectArrayList<>();
		for (Option option : module.options) {
			switch (option.info) {
				case Check _ -> components.add(new CheckComponent(option));
				case Slider _ -> components.add(new SliderComponent(option));
				case RangeSlider _ -> components.add(new RangeSliderComponent(option));
				case Combo _ -> components.add(new ComboComponent(option));
				case null, default -> components.add(new ColorComponent(option));
			}
		}
		components.add(new KeybindComponent(module));
		return components;
	}

	@Override
	protected void renderHeader(int mouseX, int mouseY) {
		boolean hovered = hovered(mouseX, mouseY);
		if (hovered) Tooltip.show(description);

		Draw.rounded(x, y, width, HEIGHT, RADIUS, headerRadius(), hovered ? Draw.CONTAINER_HIGHEST : Draw.CONTAINER_HIGH);

		int textRight = right();
		if (!(module instanceof ClickGuiModule)) {
			textRight -= CheckComponent.SWITCH_WIDTH;
			CheckComponent.drawSwitch(textRight, y + (HEIGHT - CheckComponent.SWITCH_HEIGHT) / 2, module.isEnabled());
			textRight -= 6;
		}

		boolean bound = module.getBind() != Keyboard.KEY_NONE;
		String key = listening == module ? "Press a key" : bound ? Keyboard.getKeyName(module.getBind()) : hovered ? "Set key" : null;
		keyLeft = keyRight = textRight;
		if (key != null) {
			keyLeft = keyRight - Draw.width(key) - 10;
			boolean keyHovered = hovered && mouseX >= keyLeft && mouseX < keyRight;
			drawKey(keyLeft, y + (HEIGHT - KEY_HEIGHT) / 2, key, listening == module, bound, keyHovered);
			textRight = keyLeft - 6;
		}

		String name = module.name;
		int room = textRight - x - PADDING;
		if (Draw.width(name) > room) name = Draw.trim(name, room - Draw.width("...")).stripTrailing() + "...";
		Draw.text(name, x + PADDING, Draw.textY(y, HEIGHT), module.isEnabled() ? Draw.PRIMARY : Draw.TEXT);
	}

	@Override
	protected void headerClicked(int mouseX, int mouseY, int button) {
		if (button == 2 || button == 0 && mouseX >= keyLeft && mouseX < keyRight) listening = module;
		else if (button == 0) module.toggle();
		else if (button == 1) open = !open;
	}

	@Override
	public boolean keyPressed(int key) {
		if (listening != module) return super.keyPressed(key);

		module.setBind(key == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : key);
		listening = null;
		return true;
	}
}
