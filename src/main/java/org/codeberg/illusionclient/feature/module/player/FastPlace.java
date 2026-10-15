package org.codeberg.illusionclient.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.HitResult;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.UseDelayEvent;
import org.codeberg.illusionclient.event.UseItemEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.ItemUtils;

@ModuleInfo(name = "Fast Place", category = Category.PLAYER)
@Description("Removes the delay between block placements.")
public class FastPlace extends Module {
	private static final float TICKS_PER_SECOND = 20.0F;
	private static final double DIAGONAL_RATIO = Math.sin(Math.toRadians(22.5D));
	private final Minecraft minecraft = Minecraft.getInstance();

	@Description("Smart only places on the face you started on.")
	@Combo(value = "Mode", options = {"Regular", "Smart"})
	public String mode = "Regular";

	@Description("The most blocks placed per second.")
	@Slider(value = "Maximum CPS", min = 5.0F, max = 20.0F)
	public float maximumCps = 20.0F;

	@Description("Ticks to hold use before placing speeds up.")
	@Slider(value = "Activate Delay", min = 0.0F, max = 4.0F)
	public float activateDelay = 0.0F;

	@Description("Only speeds up blocks, not other items.")
	@Check("Blocks Only")
	public boolean blocksOnly = true;

	private Direction lockedFace;
	private BlockPos lockedPos;
	private int heldTicks;
	private float remainder;
	private String details;
	private float detailsCps;
	private String detailsMode;

	@Override
	public String details() {
		if (details == null || maximumCps != detailsCps || !mode.equals(detailsMode)) {
			detailsCps = maximumCps;
			detailsMode = mode;
			details = mode + " " + number(maximumCps) + "cps";
		}
		return details;
	}

	@Override
	public void onDisable() {
		lockedFace = null;
		heldTicks = 0;
		remainder = 0.0F;
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		if (minecraft.options.useKey.isPressed()) {
			heldTicks++;
		} else {
			lockedFace = null;
			heldTicks = 0;
			remainder = 0.0F;
		}
	}

	@Listen
	public void onUseItem(UseItemEvent event) {
		boolean smart = mode.equals("Smart");
		if (!smart || event.click) lockedFace = null;
		if (!smart) return;

		HitResult target = minecraft.crosshairTarget;
		if (target == null || target.type != HitResult.Type.BLOCK) return;
		if (!ItemUtils.isBlock(minecraft.player.inventory.getSelectedItem())) return;

		if (target.face == Direction.DOWN) {
			event.cancelled = true;
		} else if (lockedFace == null || target.face == lockedFace || !reachable()) {
			lockedFace = target.face;
			lockedPos = target.getPos();
		} else if (!diagonal(target.face)) {
			event.cancelled = true;
		}
	}

	private boolean reachable() {
		BlockPos front = lockedPos.offset(lockedFace);
		BlockPos pos = minecraft.world.isAir(front) ? lockedPos : front;
		Vec3d eye = minecraft.player.getEyePosition(1.0F);
		double outward = (eye.x - pos.getX() - 0.5D) * lockedFace.getOffsetX()
			+ (eye.y - pos.getY() - 0.5D) * lockedFace.getOffsetY()
			+ (eye.z - pos.getZ() - 0.5D) * lockedFace.getOffsetZ();
		if (outward <= 0.5D) return false;

		double x = Math.max(0.0D, Math.max(pos.getX() - eye.x, eye.x - pos.getX() - 1.0D));
		double y = Math.max(0.0D, Math.max(pos.getY() - eye.y, eye.y - pos.getY() - 1.0D));
		double z = Math.max(0.0D, Math.max(pos.getZ() - eye.z, eye.z - pos.getZ() - 1.0D));
		double reach = minecraft.interactionManager.getReach();
		return x * x + y * y + z * z <= reach * reach;
	}

	private boolean diagonal(Direction face) {
		double yaw = Math.toRadians(minecraft.player.yaw);
		double x = Math.sin(yaw);
		double z = -Math.cos(yaw);
		return Math.min(Math.abs(x), Math.abs(z)) > DIAGONAL_RATIO * Math.hypot(x, z)
			&& along(face, x, z) && along(lockedFace, x, z);
	}

	private static boolean along(Direction face, double x, double z) {
		return face.getOffsetX() * x + face.getOffsetZ() * z > 0.0D;
	}

	@Listen
	public void onUseDelay(UseDelayEvent event) {
		if (blocksOnly && !ItemUtils.isBlock(minecraft.player.inventory.getSelectedItem())) return;

		float ticks = TICKS_PER_SECOND / maximumCps + remainder;
		int delay = (int) ticks;
		remainder = ticks - delay;

		int untilActive = Math.round(activateDelay) - heldTicks;
		if (untilActive > 0) {
			remainder = 0.0F;
			delay = Math.max(delay, untilActive);
		}
		event.delay = Math.min(event.delay, delay);
	}
}
