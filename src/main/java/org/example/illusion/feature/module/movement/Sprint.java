package org.example.illusion.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import org.example.illusion.event.JumpYawEvent;
import org.example.illusion.event.UpdateSprintEvent;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Sprint", category = Category.MOVEMENT)
@Description("Sprints for you without holding the sprint key.")
public class Sprint extends Module {
	private static final float SPRINT_INPUT = 0.8F;
	private static final float ITEM_USE_SLOWDOWN = 0.2F;
	private static final float MINIMUM_FOOD = 6.0F;
	private final Minecraft minecraft = Minecraft.getInstance();
	@Check("Using Item")
	public boolean usingItem = false;
	@Check("Backwards")
	public boolean backwards = false;
	@Check("Sideways")
	public boolean sideways = false;
	@Check("In Inventory")
	public boolean inInventory = false;
	private boolean sprinting;

	@Override
	public void onDisable() {
		sprinting = false;
	}

	@Listen
	public void onUpdateSprint(UpdateSprintEvent event) {
		sprinting = shouldSprint(minecraft.player);
		event.sprinting |= sprinting;
	}

	@Listen
	public void onJumpYaw(JumpYawEvent event) {
		float forward = minecraft.player.input.movementForward;
		float strafe = minecraft.player.input.movementSideways;
		if (forward < 0.0F ? !backwards : forward > 0.0F || strafe == 0.0F || !sideways) return;

		event.yaw -= (float) Math.toDegrees(Math.atan2(strafe, forward));
	}

	private boolean shouldSprint(LocalClientPlayerEntity player) {
		boolean inScreen = minecraft.screen != null;
		if (inScreen && !inInventory) return false;
		if (player.collidingHorizontally || player.hasStatusEffect(StatusEffect.BLINDNESS)) return false;
		if (player.getHungerManager().getFoodLevel() <= MINIMUM_FOOD && !player.abilities.canFly) return false;

		float input = SPRINT_INPUT;
		if (player.hasItemInUse()) {
			if (!usingItem) return false;
			if (!player.isRiding()) input *= ITEM_USE_SLOWDOWN;
		}

		float forward = player.input.movementForward;
		boolean moving;
		if (forward >= input) moving = true;
		else if (forward <= -input) moving = backwards;
		else moving = sideways && Math.abs(player.input.movementSideways) >= input;

		return moving || inScreen && sprinting;
	}
}
