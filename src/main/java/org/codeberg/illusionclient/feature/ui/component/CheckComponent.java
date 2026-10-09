package org.codeberg.illusionclient.feature.ui.component;

import org.codeberg.illusionclient.feature.config.api.Option;
import org.codeberg.illusionclient.feature.ui.Draw;

public class CheckComponent extends Component {
	private static final int SWITCH_WIDTH = 24;
	private static final int SWITCH_HEIGHT = 12;
	private static final int KNOB = SWITCH_HEIGHT - 4;

	private final Option option;

	public CheckComponent(Option option) {
		this.option = option;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		row(option.name, mouseX, mouseY);

		boolean checked = option.getBoolean();
		int switchX = right() - SWITCH_WIDTH;
		int switchY = y + (HEIGHT - SWITCH_HEIGHT) / 2;
		int knobX = checked ? switchX + SWITCH_WIDTH - KNOB - 2 : switchX + 2;

		Draw.rounded(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2.0F, checked ? Draw.accent().getRGB() : Draw.TRACK);
		Draw.rounded(knobX, switchY + 2, KNOB, KNOB, KNOB / 2.0F, checked ? -1 : Draw.MUTED);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && hovered(mouseX, mouseY)) option.set(!option.getBoolean());
	}
}
