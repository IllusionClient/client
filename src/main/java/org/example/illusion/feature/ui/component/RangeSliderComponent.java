package org.example.illusion.feature.ui.component;

import org.example.illusion.feature.config.api.Option;
import org.example.illusion.feature.config.api.RangeSlider;
import org.example.illusion.feature.ui.Draw;

public class RangeSliderComponent extends Component {
	private static final int TRACK_Y = HEIGHT;
	private static final int TRACK_HEIGHT = 4;
	private static final int KNOB = 8;

	private final Option option;
	private int dragging = -1;
	private float shownLow = Float.NaN, shownHigh = Float.NaN;
	private String text;

	public RangeSliderComponent(Option option) {
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
		RangeSlider slider = (RangeSlider) option.info;
		float min = slider.min();
		float max = slider.max();
		int width = right() - left();
		float[] range = option.get();

		if (dragging != -1) {
			float progress = Math.clamp((mouseX - left()) / (float) width, 0.0F, 1.0F);
			float value = Math.round((min + progress * (max - min)) * 10.0F) / 10.0F;
			range[dragging] = dragging == 0 ? Math.min(value, range[1]) : Math.max(value, range[0]);
		}

		if (range[0] != shownLow || range[1] != shownHigh) {
			shownLow = range[0];
			shownHigh = range[1];
			text = shownLow + " - " + shownHigh;
		}

		int low = offset(range[0]);
		int high = offset(range[1]);
		int trackY = y + TRACK_Y;
		int knobY = trackY - (KNOB - TRACK_HEIGHT) / 2;

		row(option.name, mouseX, mouseY);
		value(text);
		Draw.rounded(left(), trackY, width, TRACK_HEIGHT, 2.0F, Draw.TRACK);
		Draw.rounded(left() + low, trackY, high - low, TRACK_HEIGHT, 2.0F, Draw.accent().getRGB());
		Draw.rounded(left() + low - KNOB / 2, knobY, KNOB, KNOB, KNOB / 2.0F, -1);
		Draw.rounded(left() + high - KNOB / 2, knobY, KNOB, KNOB, KNOB / 2.0F, -1);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int button) {
		if (button != 0 || !hovered(mouseX, mouseY)) return;

		float[] range = option.get();
		int low = Math.abs(mouseX - left() - offset(range[0]));
		int high = Math.abs(mouseX - left() - offset(range[1]));
		if (low == high) dragging = mouseX < left() + offset(range[0]) ? 0 : 1;
		else dragging = low < high ? 0 : 1;
	}

	@Override
	public void mouseReleased() {
		dragging = -1;
	}

	private int offset(float value) {
		RangeSlider slider = (RangeSlider) option.info;
		return (int) ((right() - left()) * (value - slider.min()) / (slider.max() - slider.min()));
	}
}
