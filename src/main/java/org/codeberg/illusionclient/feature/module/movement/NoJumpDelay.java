package org.codeberg.illusionclient.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import org.codeberg.illusionclient.event.JumpDelayEvent;
import org.codeberg.illusionclient.event.JumpEvent;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.MoveUtils;

@ModuleInfo(name = "No Jump Delay", category = Category.MOVEMENT)
@Description("Removes the jump cooldown while holding the jump key.")
public class NoJumpDelay extends Module {
	private static final double JUMP_RISE = 0.42D;
	private static final double SPRINT_JUMP_BOOST = 0.2D;
	private static final double INPUT_SCALE = 0.98D;
	private static final double LOOKAHEAD = 1.5D;
	private static final double LOOKAHEAD_STEP = 0.25D;
	private final Minecraft minecraft = Minecraft.getInstance();
	@Combo(value = "Mode", options = {"Regular", "Smart"})
	public String mode = "Regular";

	@Override
	public String details() {
		return mode;
	}

	@Listen
	public void onJumpDelay(JumpDelayEvent event) {
		event.ticks = 0;
	}

	@Listen
	public void onJump(JumpEvent event) {
		if (mode.equals("Smart") && shouldWaitForStep()) event.cancelled = true;
	}

	private boolean shouldWaitForStep() {
		LocalClientPlayerEntity player = minecraft.player;
		double[] input = MoveUtils.inputDirection(player);
		double speed = player.getSpeed() * INPUT_SCALE / Math.max(1.0D, Math.hypot(input[0], input[1]));
		double x = player.velocityX + input[0] * speed;
		double z = player.velocityZ + input[1] * speed;
		if (player.isSprinting()) {
			double yaw = Math.toRadians(player.yaw);
			x -= Math.sin(yaw) * SPRINT_JUMP_BOOST;
			z += Math.cos(yaw) * SPRINT_JUMP_BOOST;
		}
		double distance = Math.hypot(x, z);
		if (distance < 1.0E-4D) return false;
		if (blocked(player, x, JUMP_RISE, z) && !blocked(player, x, player.stepHeight, z)) return false;

		x /= distance;
		z /= distance;
		for (double ahead = LOOKAHEAD_STEP; ahead <= LOOKAHEAD; ahead += LOOKAHEAD_STEP) {
			if (blocked(player, x * ahead, 0.0D, z * ahead)) {
				return !blocked(player, x * ahead, player.stepHeight, z * ahead);
			}
		}
		return false;
	}

	private boolean blocked(LocalClientPlayerEntity player, double x, double y, double z) {
		return !minecraft.world.getCollisions(player, player.getShape().moved(x, y, z)).isEmpty();
	}
}
