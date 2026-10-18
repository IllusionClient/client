package org.codeberg.illusionclient.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.RangeSlider;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.AimPoint;
import org.codeberg.illusionclient.util.CombatUtils;
import org.codeberg.illusionclient.util.RotationUtils;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "Aim Assist", category = Category.COMBAT)
@Description("Moves your crosshair onto players.")
public class AimAssist extends Module {
	private static final float PREDICT_TICKS = 3.0F;
	private static final long CLICK_WINDOW = 300L;
	private static final float RESIST_LIMIT = 15.0F;
	private static final float RESIST_DECAY = 30.0F;
	private static final long RESIST_COOLDOWN = 1000L;
	private static final double CLOSE_RANGE = 3.5D;
	private final Minecraft minecraft = Minecraft.getInstance();

	@Description("Regular eases in, Linear is steady, Lock-on snaps.")
	@Combo(value = "Mode", options = {"Regular", "Linear", "Lock-on"})
	public String mode = "Regular";

	@Description("Horizontal turn speed.")
	@Slider(value = "Horizontal Speed", min = 0.0F, max = 100.0F)
	public float horizontalSpeed = 30.0F;

	@Description("Vertical turn speed.")
	@Slider(value = "Vertical Speed", min = 0.0F, max = 100.0F)
	public float verticalSpeed = 15.0F;

	@Description("Pulls horizontal aim toward your crosshair. 0 is center.")
	@Slider(value = "Horizontal Multipoint", min = 0.0F, max = 100.0F)
	public float horizontalMultipoint = 0.0F;

	@Description("Pulls vertical aim toward your crosshair. 0 is center.")
	@Slider(value = "Vertical Multipoint", min = 0.0F, max = 100.0F)
	public float verticalMultipoint = 0.0F;

	@Description("Aims ahead of moving targets.")
	@Slider(value = "Predict", min = 0.0F, max = 100.0F)
	public float predict = 0.0F;

	@Description("Wanders the aim point around the hitbox.")
	@Slider(value = "Randomization", min = 0.0F, max = 100.0F)
	public float randomization = 50.0F;

	@Description("Single keeps one target, Switch picks the best each frame.")
	@Combo(value = "Target Mode", options = {"Single", "Switch"})
	public String targetMode = "Single";

	@Description("Keeps the target after it leaves your FOV.")
	@Requires(value = "targetMode", is = "Single")
	@Check("Increased FOV While Locked")
	public boolean increasedFov = false;

	@Description("Which target to prefer.")
	@Requires(value = "targetMode", is = "Switch")
	@Combo(value = "Sort By", options = {"Distance", "Health", "Hurt time", "Aim angle"})
	public String sortBy = "Distance";

	@Description("Only aims at players within this distance.")
	@RangeSlider(value = "Range", min = 0.0F, max = 8.0F)
	public float[] range = {0.0F, 4.5F};

	@Description("Aims within the max angle, stops inside the min angle.")
	@RangeSlider(value = "FOV", min = 0.0F, max = 360.0F)
	public float[] fov = {0.0F, 90.0F};

	@Description("Only aims while you are sprinting.")
	@Check("Sprinting")
	public boolean sprinting = false;

	@Description("Only aims while you are clicking.")
	@Check("Mouse Pressed")
	public boolean mousePressed = true;

	@Description("Only aims while you move your mouse, and never further than you do.")
	@Check("Mouse Moved")
	public boolean mouseMoved = false;

	@Description("Drops the target briefly when you aim away.")
	@Check("Stop When Resisting")
	public boolean resisting = false;

	@Description("Stops aiming while you are mining a block.")
	@Check("Disable While Breaking Blocks")
	public boolean breakingBlocks = true;

	@Description("Skips invisible players.")
	@Check("Ignore Invisibles")
	public boolean ignoreInvisibles = true;

	@Description("Skips players you can't see through blocks.")
	@Check("Not Behind Blocks")
	public boolean notBehindBlocks = true;

	private PlayerEntity target;
	private long lastFrame;
	private long lastClick;
	private PlayerEntity resisted;
	private long resistedUntil;
	private float resist;
	private float yawRemainder;
	private float pitchRemainder;

	private static int mouseCounts(float total, float diff, float gcd) {
		int limit = (int) (diff / gcd);
		return Math.clamp((int) (total / gcd), Math.min(limit, 0), Math.max(limit, 0));
	}

	private static float shrink(float diff, float deadzone) {
		return Math.copySign(Math.max(0.0F, Math.abs(diff) - deadzone), diff);
	}

	private static float noise(double x) {
		long cell = (long) Math.floor(x);
		float t = (float) (x - cell);
		float from = gradient(cell) * t;
		float to = gradient(cell + 1L) * (t - 1.0F);
		return 2.0F * (from + (to - from) * t * t * (3.0F - 2.0F * t));
	}

	private static float gradient(long cell) {
		return Long.hashCode(cell * 0x9E3779B97F4A7C15L) / (float) Integer.MAX_VALUE;
	}

	private static float wander(float min, float max, float period, int channel) {
		float noise = noise(System.nanoTime() / 1.0E9D / period + channel * 1000.0D);
		return min + (max - min) * (noise + 1.0F) / 2.0F;
	}

	private static float step(float diff, float speed, float delta, String mode) {
		return switch (mode) {
			case "Lock-on" -> diff;
			case "Linear" -> {
				float limit = speed * 6.0F * delta * Math.min(1.0F, 0.4F + Math.abs(diff) / 15.0F);
				yield Math.clamp(diff, -limit, limit);
			}
			default -> diff * (1.0F - (float) Math.pow(1.0F - Math.min(1.0F, speed / 100.0F), delta * 20.0F));
		};
	}

	@Override
	public String details() {
		return mode;
	}

	@Override
	public void onDisable() {
		target = resisted = null;
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		target = resisted = null;
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		long now = System.nanoTime();
		float delta = Math.min(0.1F, (now - lastFrame) / 1.0E9F);
		lastFrame = now;

		if (Mouse.isButtonDown(0)) lastClick = System.currentTimeMillis();

		LocalClientPlayerEntity player = minecraft.player;
		if (player == null || findTarget(player.yaw, player.pitch) == null) {
			yawRemainder = pitchRemainder = resist = 0.0F;
			return;
		}

		float[] goal = RotationUtils.rotationTo(aimPoint(event.partialTicks()));
		float yawDiff = MathHelper.wrapDegrees(goal[0] - player.yaw);
		float pitchDiff = goal[1] - player.pitch;

		float sensitivity = minecraft.options.mouseSensitivity * 0.6F + 0.2F;
		float scale = sensitivity * sensitivity * sensitivity * 8.0F;
		float gcd = scale * 0.15F;

		if (resisting && resisted(minecraft.mouse.x * gcd, yawDiff, delta)) return;

		yawDiff = shrink(yawDiff, fov[0] / 2.0F);
		pitchDiff = shrink(pitchDiff, fov[0] / 2.0F);
		if (yawDiff == 0.0F && pitchDiff == 0.0F) return;

		boolean lockOn = mode.equals("Lock-on");
		if (!lockOn) pitchDiff *= Math.max(0.0F, 1.0F - Math.abs(yawDiff) / wander(40.0F, 120.0F, 5.0F, 0));

		float drift = wander(0.95F, 1.05F, 60.0F, 1);
		float yawStep = step(yawDiff, horizontalSpeed * drift * wander(0.9F, 1.1F, 1.0F, 2), delta, mode);
		float pitchStep = step(pitchDiff, verticalSpeed * drift * wander(0.9F, 1.1F, 1.0F, 3), delta, mode);

		float yawTotal = yawStep + yawRemainder;
		yawRemainder = yawTotal % gcd;
		int yawCounts = mouseCounts(yawTotal, yawDiff, gcd);
		if (mouseMoved) yawCounts = Math.clamp(yawCounts, -Math.abs(minecraft.mouse.x), Math.abs(minecraft.mouse.x));

		if (!lockOn && yawCounts != 0) pitchStep += Math.abs(yawCounts) * gcd * wander(-0.1F, 0.1F, 1.0F, 4);
		float pitchTotal = pitchStep + pitchRemainder;
		pitchRemainder = pitchTotal % gcd;
		int pitchCounts = mouseCounts(pitchTotal, pitchDiff, gcd);
		if (mouseMoved) pitchCounts = Math.clamp(pitchCounts, -Math.abs(minecraft.mouse.y), Math.abs(minecraft.mouse.y));
		if (yawCounts != 0 || pitchCounts != 0) player.updateLocalPlayerCamera(yawCounts * scale, -pitchCounts * scale);
	}

	private boolean resisted(float mouseYaw, float yawDiff, float delta) {
		float away = -mouseYaw * Math.signum(yawDiff);
		resist = Math.max(0.0F, resist + Math.max(0.0F, away) - RESIST_DECAY * delta);
		if (resist < RESIST_LIMIT) return false;

		resisted = target;
		resistedUntil = System.currentTimeMillis() + RESIST_COOLDOWN;
		target = null;
		yawRemainder = pitchRemainder = resist = 0.0F;
		return true;
	}

	private PlayerEntity findTarget(float yaw, float pitch) {
		if (!active()) return target = null;

		boolean single = targetMode.equals("Single");
		if (single && target != null && valid(target, yaw, pitch, increasedFov)) return target;

		String sort = single ? "Aim angle" : sortBy;
		PlayerEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (PlayerEntity player : minecraft.world.players) {
			if (!valid(player, yaw, pitch, false)) continue;

			double score = score(player, sort, yaw, pitch);
			if (score < bestScore) {
				best = player;
				bestScore = score;
			}
		}
		return target = best;
	}

	private boolean active() {
		LocalClientPlayerEntity player = minecraft.player;
		if (player == null || minecraft.world == null || minecraft.screen != null || !player.isAlive()) return false;
		if (sprinting && !player.isSprinting()) return false;
		if (mousePressed && System.currentTimeMillis() - lastClick > CLICK_WINDOW) return false;
		if (breakingBlocks && minecraft.interactionManager.isMiningBlock()) return false;
		return Weapons.isWeapons(player.getItemInHand());
	}

	private boolean valid(PlayerEntity player, float yaw, float pitch, boolean anyFov) {
		LocalClientPlayerEntity self = minecraft.player;
		if (player == self || !player.isAlive() || !CombatUtils.isTarget(player)) return false;
		if (ignoreInvisibles && player.isInvisible()) return false;
		if (player == resisted && System.currentTimeMillis() < resistedUntil) return false;

		double distance = CombatUtils.distanceTo(player);
		if (distance < range[0] || distance > range[1]) return false;
		if (!anyFov && RotationUtils.angleTo(player, yaw, pitch) * 2.0D > fov[1]) return false;
		return !notBehindBlocks || self.canSee(player);
	}

	private double score(PlayerEntity player, String sort, float yaw, float pitch) {
		return switch (sort) {
			case "Health" -> player.getHealth();
			case "Hurt time" -> player.damagedTimer;
			case "Aim angle" -> RotationUtils.angleTo(player, yaw, pitch);
			default -> CombatUtils.distanceTo(player);
		};
	}

	private Vec3d aimPoint(float tickDelta) {
		LocalClientPlayerEntity self = minecraft.player;
		double selfX = (self.x - self.prevX) * (tickDelta - 1.0F);
		double selfY = (self.y - self.prevY) * (tickDelta - 1.0F);
		double selfZ = (self.z - self.prevZ) * (tickDelta - 1.0F);
		double lead = tickDelta - 1.0F + predict / 100.0F * PREDICT_TICKS;
		Box box = target.getShape().moved(
			(target.x - target.prevX) * lead - selfX,
			(target.y - target.prevY) * (tickDelta - 1.0F) - selfY,
			(target.z - target.prevZ) * lead - selfZ);

		Vec3d eye = self.getEyePosition(1.0F);
		float near = (float) Math.min(1.0D, CombatUtils.distanceTo(box) / CLOSE_RANGE);
		float spread = randomization / 100.0F * (0.1F + 0.9F * near);
		float multipoint = horizontalMultipoint * near;
		double seed = 0.1D * (eye.x + eye.y + eye.z + (box.minX + box.maxX + box.minY + box.maxY + box.minZ + box.maxZ) / 2.0D);
		double x = AimPoint.axis(eye.x, (box.minX + box.maxX) / 2.0D, box.minX, box.maxX, multipoint, spread * 0.4F, noise(seed));
		double y = AimPoint.axis(eye.y, box.minY + target.getEyeHeight(), box.minY, box.maxY, verticalMultipoint, spread * 0.3F, noise(seed + 10000.0D));
		double z = AimPoint.axis(eye.z, (box.minZ + box.maxZ) / 2.0D, box.minZ, box.maxZ, multipoint, spread * 0.4F, noise(seed + 20000.0D));
		return new Vec3d(x, y, z);
	}
}
