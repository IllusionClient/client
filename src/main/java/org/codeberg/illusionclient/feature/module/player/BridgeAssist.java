package org.codeberg.illusionclient.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.InputEvent;
import org.codeberg.illusionclient.event.UseItemEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.input.InputState;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.ItemUtils;
import org.codeberg.illusionclient.util.MoveUtils;

import java.util.Random;

@ModuleInfo(name = "Bridge Assist", category = Category.PLAYER)
@Description("Sneaks for you at block edges.")
public class BridgeAssist extends Module {
	private static final float LOOKING_DOWN_PITCH = 45.0F;
	private static final double DIAGONAL_RATIO = Math.sin(Math.toRadians(22.5D));
	private static final double SECOND_EDGE_REACH = 0.2D;
	private static final float MS_PER_TICK = 50.0F;
	private static final float BRIDGE_ANGLE = 45.0F;
	private static final float BRIDGE_ANGLE_TOLERANCE = 7.5F;
	private static final float DIAGONAL_TOLERANCE = 10.0F;
	private static final float MIN_TURN = 30.0F;
	private static final double ALIGN_TOLERANCE = 0.05D;
	private static final double MOVING_SPEED = 0.05D;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Random random = new Random();

	@Description("How far past the edge before sneaking.")
	@Slider(value = "Edge Offset", min = 0.0F, max = 30.0F)
	public float edgeOffset = 15.0F;

	@Description("Sneak time after you're back on the block.")
	@Slider(value = "Unsneak Delay", min = 50.0F, max = 500.0F)
	public float unsneakDelay = 100.0F;

	@Description("Swaps to hotbar blocks when your hand has none.")
	@Combo(value = "Select Blocks", options = {"No", "On depletion", "Always"})
	public String selectBlocks = "No";

	@Description("Varies the edge offset and unsneak delay.")
	@Check("Randomize")
	public boolean randomize = false;

	@Description("Sneaks while you turn to a bridging angle, until you stop turning.")
	@Check("Pre-Sneak")
	public boolean preSneak = false;

	@Description("Strafes you onto the block diagonal when bridging diagonally backwards.")
	@Check("Strafe Align")
	public boolean strafeAlign = false;

	@Description("Also sneaks as you come down from a jump.")
	@Check("Sneak On Jump")
	public boolean sneakOnJump = false;

	@Description("How high above your takeoff point to start sneaking on the way down.")
	@Requires("sneakOnJump")
	@Slider(value = "Jump Sneak Height", min = 0.0F, max = 1.25F)
	public float jumpSneakHeight = 0.8F;

	@Description("Only sneaks on jump while you move.")
	@Requires("sneakOnJump")
	@Check("Only While Moving")
	public boolean onlyWhileMoving = true;

	@Description("Waits for the second edge when bridging diagonally.")
	@Check("Avoid Double-Sneaking")
	public boolean avoidDoubleSneaking = false;

	@Description("Only sneaks at edges while you hold sneak.")
	@Check("Sneak Key Pressed")
	public boolean sneakKeyPressed = false;

	@Description("Only works while you hold blocks.")
	@Check("Holding Blocks")
	public boolean holdingBlocks = true;

	@Description("Only works while you look down.")
	@Check("Looking Down")
	public boolean lookingDown = false;

	@Description("Only works when not pressing forward.")
	@Check("Not Moving Forward")
	public boolean notMovingForward = false;

	private boolean edge;
	private boolean placed;
	private float offset;
	private boolean sneaking;
	private int unsneakTicks;
	private float jitter;
	private int blockSlot = -1;
	private String details;
	private float detailsDelay;
	private boolean tracking;
	private float lastYaw;
	private float lastPitch;
	private float turned;
	private int stillTicks;
	private int preSneakTicks = 2;
	private boolean preSneaking;
	private double groundY;

	@Override
	public String details() {
		if (details == null || unsneakDelay != detailsDelay) {
			detailsDelay = unsneakDelay;
			details = number(unsneakDelay) + "ms";
		}
		return details;
	}

	@Override
	public void onDisable() {
		edge = sneaking = placed = false;
		unsneakTicks = 0;
		blockSlot = -1;
		tracking = preSneaking = false;
		turned = 0.0F;
		stillTicks = 0;
	}

	@Listen
	public void onInput(InputEvent event) {
		LocalClientPlayerEntity player = minecraft.player;
		PlayerInventory inventory = player.inventory;
		boolean active = active(player);
		trackRotation(player);
		if (player.onGround) groundY = player.y;

		boolean depleted = inventory.getSelectedItem() == null && inventory.selectedSlot == blockSlot;
		if (active && !ItemUtils.isBlock(inventory.getSelectedItem())
			&& (selectBlocks.equals("Always") || depleted && selectBlocks.equals("On depletion"))
			&& IllusionClient.MODULES.claimSlot(this)) {
			for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
				if (!ItemUtils.isBlock(inventory.items[slot])) continue;
				inventory.selectedSlot = slot;
				break;
			}
			IllusionClient.MODULES.releaseSlot(this);
		}

		boolean holding = ItemUtils.isBlock(inventory.getSelectedItem());
		blockSlot = holding ? inventory.selectedSlot : -1;
		if (!active || holdingBlocks && !holding) {
			edge = sneaking = placed = false;
			unsneakTicks = 0;
			return;
		}

		float spread = randomize ? jitter : 0.0F;
		offset = edgeOffset / 100.0F * (1.0F - spread * 0.3F);
		edge = player.onGround && overEdge(player);
		if (!edge) placed = false;
		boolean jump = sneakOnJump && !player.onGround && player.velocityY < 0.0D
			&& player.y - groundY <= jumpSneakHeight * (1.0F - spread * 0.3F)
			&& (!onlyWhileMoving || moving(player));
		if (edge || jump) unsneakTicks = Math.max(1, Math.round(unsneakDelay * (1.0F + spread * 0.5F) / MS_PER_TICK));

		boolean sneak = edge || jump || unsneakTicks-- > 0 || preSneak && preSneaking;
		if (sneaking && !sneak) jitter = random.nextFloat();
		sneaking = sneak;
		InputState client = IllusionClient.INPUT.client;
		client.sneak = sneak || !sneakKeyPressed && client.sneak;
		if (strafeAlign) align(player);
	}

	@Listen
	public void onUseItem(UseItemEvent event) {
		if (!avoidDoubleSneaking || !edge || placed) return;

		LocalClientPlayerEntity player = minecraft.player;
		double[] direction = MoveUtils.inputDirection(player);
		double x = direction[0];
		double z = direction[1];

		double backX = -Math.signum(x) * player.width;
		double backZ = -Math.signum(z) * player.width;
		boolean pastX = !unsupported(player, backX, 0.0D);
		boolean pastZ = !unsupported(player, 0.0D, backZ);

		if (Math.min(Math.abs(x), Math.abs(z)) > DIAGONAL_RATIO * Math.hypot(x, z) && pastX != pastZ
			&& unsupported(player, pastX ? backX : Math.signum(x) * SECOND_EDGE_REACH, pastX ? Math.signum(z) * SECOND_EDGE_REACH : backZ)) {
			event.cancelled = true;
		} else {
			placed = true;
		}
	}

	private boolean active(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		if (minecraft.screen != null) return false;
		if (sneakKeyPressed && !input.sneak) return false;
		if (lookingDown && player.pitch < LOOKING_DOWN_PITCH) return false;
		return !notMovingForward || !input.forward;
	}

	private void trackRotation(LocalClientPlayerEntity player) {
		float yawDelta = MathHelper.wrapDegrees(player.yaw - lastYaw);
		float pitchDelta = player.pitch - lastPitch;
		boolean rotating = tracking && (yawDelta != 0.0F || pitchDelta != 0.0F);
		tracking = true;
		lastYaw = player.yaw;
		lastPitch = player.pitch;

		if (!rotating) {
			if (++stillTicks >= preSneakTicks) {
				turned = 0.0F;
				preSneaking = false;
			}
			return;
		}

		stillTicks = 0;
		turned += (float) Math.hypot(yawDelta, pitchDelta);
		if (!preSneaking && turned >= MIN_TURN && player.onGround && player.pitch >= LOOKING_DOWN_PITCH
			&& angleFrom(player.yaw, BRIDGE_ANGLE, 0.0F) <= BRIDGE_ANGLE_TOLERANCE) {
			preSneaking = true;
			preSneakTicks = 2 + random.nextInt(2);
		}
	}

	private void align(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		if (!input.back || input.forward || input.left || input.right) return;
		if (angleFrom(player.yaw, 90.0F, 45.0F) > DIAGONAL_TOLERANCE) return;

		double[] back = MoveUtils.inputDirection(player.yaw, -1.0F, 0.0F);
		double slope = Math.signum(back[0]) * Math.signum(back[1]);
		double line = player.x + player.velocityX - slope * (player.z + player.velocityZ);
		double error = line - Math.round(line);
		if (Math.abs(error) / Math.sqrt(2.0D) <= ALIGN_TOLERANCE) return;

		double[] left = MoveUtils.inputDirection(player.yaw, 0.0F, 1.0F);
		double side = -error * left[0] + error * slope * left[1];
		InputState client = IllusionClient.INPUT.client;
		client.left = side > 0.0D;
		client.right = side < 0.0D;
	}

	private static float angleFrom(float yaw, float step, float target) {
		float angle = (yaw % step + step) % step;
		float distance = Math.abs(angle - target);
		return Math.min(distance, step - distance);
	}

	private static boolean moving(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		return input.forward || input.back || input.left || input.right
			|| Math.hypot(player.velocityX, player.velocityZ) > MOVING_SPEED;
	}

	private boolean overEdge(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.client;
		float forward = input.forwardAxis();
		float sideways = input.sidewaysAxis();
		double acceleration = player.getSpeed() / Math.max(1.0D, Math.hypot(forward, sideways));
		double[] direction = MoveUtils.inputDirection(player.yaw, forward, sideways);
		return unsupported(player, player.velocityX + direction[0] * acceleration, player.velocityZ + direction[1] * acceleration);
	}

	private boolean unsupported(LocalClientPlayerEntity player, double x, double z) {
		double inset = player.width / 2.0F - offset;
		Box feet = player.getShape().contract(inset, 0.0D, inset).moved(x, -1.0D, z);
		return minecraft.world.getCollisions(player, feet).isEmpty();
	}
}
