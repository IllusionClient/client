package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Option;

import java.util.Arrays;

public class ComboComponent extends OptionComponent {
	public ComboComponent(Option option) {
		super(option);
	}

	@Override
	public void render(int mouseX, int mouseY) {
		row(option.name, mouseX, mouseY);
		value(option.get());
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button > 1 || !hovered(mouseX, mouseY)) return;

		String[] options = ((Combo) option.info).options();
		int step = button == 0 ? 1 : options.length - 1;
		option.set(options[(Arrays.asList(options).indexOf(option.get()) + step) % options.length]);
	}
}
