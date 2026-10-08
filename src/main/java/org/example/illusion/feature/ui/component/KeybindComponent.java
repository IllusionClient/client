package org.example.illusion.feature.ui.component;

import org.example.illusion.feature.module.api.Module;
import org.lwjgl.input.Keyboard;

public class KeybindComponent extends Component {
	private final Module module;
	private boolean binding;

	public KeybindComponent(Module module) {
		this.module = module;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		row("Key", mouseX, mouseY);
		value(binding ? "..." : Keyboard.getKeyName(module.getBind()));
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		binding = button == 0 && hovered(mouseX, mouseY);
	}

	@Override
	public boolean keyPressed(int key) {
		if (!binding) return false;

		module.setBind(key == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : key);
		binding = false;
		return true;
	}
}
