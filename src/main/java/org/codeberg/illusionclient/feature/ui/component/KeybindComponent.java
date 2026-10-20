package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.module.api.Module;
import org.lwjgl.input.Keyboard;

public class KeybindComponent extends Component {
	private static Module listening;

	private final Module module;

	public KeybindComponent(Module module) {
		this.module = module;
	}

	public static boolean listening(Module module) {
		return listening == module;
	}

	public static void listen(Module module) {
		listening = module;
	}

	public static void stopListening() {
		listening = null;
	}

	public static boolean keyBound(int key) {
		return bind(key == Keyboard.KEY_ESCAPE ? Keyboard.KEY_NONE : key);
	}

	public static boolean mouseBound(int button) {
		return bind(button == 0 ? Keyboard.KEY_NONE : Module.mouseBind(button));
	}

	private static boolean bind(int bind) {
		if (listening == null) return false;

		listening.setBind(bind);
		listening = null;
		return true;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		row("Key", mouseX, mouseY);
		value(listening(module) ? "..." : module.bindName());
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && hovered(mouseX, mouseY)) listen(module);
	}
}
