package org.example.illusion.feature.ui.component;

import org.example.illusion.feature.config.api.Option;
import org.example.illusion.feature.config.api.Slider;
import org.example.illusion.feature.ui.Draw;

public class SliderComponent extends Component {
	private static final int TRACK_Y = HEIGHT;
	private static final int TRACK_HEIGHT = 4;
	private static final int KNOB = 8;

	private final Option option;
	private boolean dragging;
	private float shown = Float.NaN;
	private String text;

	public SliderComponent(Option option) {
		this.option = option;
	}

	@Override
	public int height() {
		return TRACK_Y + TRACK_HEIGHT + PADDING;
	}

	@Override
	protected boolean hovered(int mouseX, int mouseY) {
		return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + height();
	}

	@Override
	public void render(int mouseX, int mouseY) {
		Slider slider = (Slider) option.info;
		float min = slider.min();
		float max = slider.max();
		int width = right() - left();

		if (dragging) {
			float progress = Math.clamp((mouseX - left()) / (float) width, 0.0F, 1.0F);
			option.set(Math.round((min + progress * (max - min)) * 10.0F) / 10.0F);
		}

		float value = option.getFloat();
		if (value != shown) {
			shown = value;
			text = String.valueOf(value);
		}
		int filled = (int) (width * (value - min) / (max - min));
		int trackY = y + TRACK_Y;

		row(option.name, mouseX, mouseY);
		value(text);
		Draw.rounded(left(), trackY, width, TRACK_HEIGHT, 2.0F, Draw.TRACK);
		Draw.rounded(left(), trackY, filled, TRACK_HEIGHT, 2.0F, Draw.accent().getRGB());
		Draw.rounded(left() + filled - KNOB / 2, trackY - (KNOB - TRACK_HEIGHT) / 2, KNOB, KNOB, KNOB / 2.0F, -1);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button == 0 && hovered(mouseX, mouseY)) dragging = true;
	}

	@Override
	public void mouseReleased() {
		dragging = false;
	}
}
