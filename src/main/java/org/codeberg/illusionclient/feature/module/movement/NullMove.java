package org.codeberg.illusionclient.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import org.codeberg.illusionclient.event.MoveInputEvent;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Null Move", category = Category.MOVEMENT)
@Description("Also known as SOCD / Null Binds.")
public class NullMove extends Module {
	private final Minecraft minecraft = Minecraft.getInstance();

	private final Axis forward = new Axis();
	private final Axis sideways = new Axis();

	@Override
	public void onDisable() {
		forward.reset();
		sideways.reset();
	}

	@Listen
	public void onMoveInput(MoveInputEvent event) {
		GameOptions options = minecraft.options;
		event.forward = forward.resolve(options.forwardKey.isPressed(), options.backKey.isPressed());
		event.sideways = sideways.resolve(options.leftKey.isPressed(), options.rightKey.isPressed());
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
