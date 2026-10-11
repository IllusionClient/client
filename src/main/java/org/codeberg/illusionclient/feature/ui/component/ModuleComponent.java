package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.*;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.feature.ui.Tooltip;

public class ModuleComponent extends Group {
	private final Module module;

	public ModuleComponent(Module module) {
		this.module = module;
		this.description = module.description;

		for (Option option : module.options) {
			switch (option.info) {
				case Check _ -> children.add(new CheckComponent(option));
				case Slider _ -> children.add(new SliderComponent(option));
				case RangeSlider _ -> children.add(new RangeSliderComponent(option));
				case Combo _ -> children.add(new ComboComponent(option));
				case null, default -> children.add(new ColorComponent(option));
			}
		}
		children.add(new KeybindComponent(module));
	}

	@Override
	protected void renderHeader(int mouseX, int mouseY) {
		boolean hovered = hovered(mouseX, mouseY);
		int accent = Draw.accent().getRGB();
		if (hovered) Tooltip.show(description);

		Draw.rounded(x, y, WIDTH, HEIGHT, 0.0F, headerRadius(), hovered ? Draw.BACKGROUND_HOVER : Draw.BACKGROUND_DARK);
		Draw.rect(x, y, WIDTH, 2, Draw.DIVIDER);
		Draw.text(module.name, x + PADDING, y + TEXT_Y, module.isEnabled() ? accent : -1);
		Draw.textRight(open ? "-" : "+", right(), y + TEXT_Y, hovered ? accent : Draw.MUTED);
	}

	@Override
	protected void headerClicked(int mouseX, int mouseY, int button) {
		if (button == 0) module.toggle();
		else if (button == 1) open = !open;
	}
}
