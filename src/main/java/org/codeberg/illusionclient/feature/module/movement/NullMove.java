package org.codeberg.illusionclient.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.InputEvent;
import org.codeberg.illusionclient.feature.input.InputState;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Null Move", category = Category.MOVEMENT)
@Description("Opposite keys move you toward the last one pressed (SOCD).")
public class NullMove extends Module {
	private final Axis forward = new Axis();
	private final Axis sideways = new Axis();

	@Override
	public void onDisable() {
		forward.reset();
		sideways.reset();
	}

	@Listen
	public void onInput(InputEvent event) {
		InputState player = IllusionClient.INPUT.player;
		InputState client = IllusionClient.INPUT.client;
		float forward = this.forward.resolve(player.forward, player.back);
		float sideways = this.sideways.resolve(player.left, player.right);
		client.forward = forward > 0.0F;
		client.back = forward < 0.0F;
		client.left = sideways > 0.0F;
		client.right = sideways < 0.0F;
	}

	private static final class Axis {
		private boolean positive;
		private boolean negative;
		private float latest;

		float resolve(boolean positive, boolean negative) {
			if (positive && !this.positive) latest = 1.0F;
			if (negative && !this.negative) latest = -1.0F;
			this.positive = positive;
			this.negative = negative;

			if (positive && negative) return latest;
			return positive ? 1.0F : negative ? -1.0F : 0.0F;
		}

		void reset() {
			positive = negative = false;
			latest = 0.0F;
		}
	}
}
