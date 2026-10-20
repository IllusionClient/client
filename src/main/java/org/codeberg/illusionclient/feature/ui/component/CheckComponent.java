package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.Option;
import org.codeberg.illusionclient.feature.ui.Draw;

public class CheckComponent extends OptionComponent {
	public static final int SWITCH_WIDTH = 24;
	public static final int SWITCH_HEIGHT = 12;
	private static final int KNOB = SWITCH_HEIGHT - 4;

	public CheckComponent(Option option) {
		super(option);
	}

	@Override
	public void render(int mouseX, int mouseY) {
		row(option.name, mouseX, mouseY);

		drawSwitch(right() - SWITCH_WIDTH, y + (HEIGHT - SWITCH_HEIGHT) / 2, option.getBoolean());
	}

	public static void drawSwitch(int x, int y, boolean checked) {
		int knobX = checked ? x + SWITCH_WIDTH - KNOB - 2 : x + 2;
		if (checked) {
			Draw.rounded(x, y, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, Draw.accent().getRGB());
		} else {
			Draw.rounded(x, y, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, Draw.FAINT);
			Draw.rounded(x + 1, y + 1, SWITCH_WIDTH - 2, SWITCH_HEIGHT - 2, SWITCH_HEIGHT / 2.0F - 1, Draw.CONTAINER_HIGHEST);
		}
		Draw.rounded(knobX, y + 2, KNOB, KNOB, KNOB / 2.0F, checked ? Draw.ON_ACCENT : Draw.FAINT);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && hovered(mouseX, mouseY)) option.set(!option.getBoolean());
	}
}
