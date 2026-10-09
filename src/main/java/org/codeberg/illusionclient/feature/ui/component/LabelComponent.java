package org.codeberg.illusionclient.feature.ui.component;

import net.minecraft.client.Minecraft;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.ui.Draw;

import java.util.List;

public class LabelComponent extends Component {
	private static final int LINE_HEIGHT = 10;

	private final List<String> lines;
	private final int color;

	public LabelComponent(Description description) {
		this.lines = Minecraft.getInstance().textRenderer.split(description.value(), WIDTH - PADDING * 3);
		this.color = description.type().color;
	}

	@Override
	public int height() {
		return HEIGHT + (lines.size() - 1) * LINE_HEIGHT;
	}

	@Override
	public void render(int mouseX, int mouseY) {
		Draw.rounded(x, y, WIDTH, height(), 0.0F, bottom ? RADIUS : 0.0F, Draw.BACKGROUND);
		Draw.rect(left(), y, right() - left(), 2, Draw.DIVIDER);

		for (int i = 0; i < lines.size(); i++) {
			Draw.text(lines.get(i), left(), y + TEXT_Y + i * LINE_HEIGHT, color);
		}
	}
}
