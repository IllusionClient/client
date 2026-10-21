package org.codeberg.illusionclient.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.util.math.BlockPos;
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
	private static final double SECOND_EDGE_REACH = 0.5D;
	private static final float MS_PER_TICK = 50.0F;
	private static final float BRIDGE_ANGLE = 45.0F;
	private static final float BRIDGE_ANGLE_TOLERANCE = 7.5F;
	private static final float DIAGONAL_TOLERANCE = 10.0F;
	private static final float MIN_TURN = 30.0F;
	private static final int STILL_TICKS = 3;
	private static final double ALIGN_START = 0.15D;
	private static final float INPUT_SCALE = 0.98F;
	private static final float SNEAK_SCALE = 0.3F;
	private static final double AIR_SPEED = 0.02D;
	private static final double GRAVITY = 0.08D;
	private static final double VERTICAL_DRAG = 0.98D;
	private static final double AIR_DRAG = 0.91D;
	private static final double GROUND_DRAG = 0.6D * AIR_DRAG;
	private static final int MAX_FALL_TICKS = 20;
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

	@Description("Sneaks as you turn to bridge near an edge, until edge sneaking takes over.")
	@Check("Pre-Sneak")
	public boolean preSneak = false;

	@Description("How close to an open edge you need to be for pre-sneak.")
	@Requires("preSneak")
	@Slider(value = "Pre-Sneak Distance", min = 0.0F, max = 0.5F)
	public float preSneakDistance = 0.5F;

	@Description("Strafes you onto the block diagonal when bridging diagonally backwards.")
	@Check("Strafe Align")
	public boolean strafeAlign = false;

	@Description("Also sneaks as you come down from a jump that would land you past an edge.")
	@Check("Sneak On Jump")
	public boolean sneakOnJump = false;

	@Description("How high above your takeoff point to start sneaking on the way down.")
	@Requires("sneakOnJump")
	@Slider(value = "Jump Sneak Height", min = 0.0F, max = 1.25F)
	public float jumpSneakHeight = 0.8F;

	@Description("Only sneaks on jump while you move in any direction but forward.")
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
	private boolean preSneaking;
	private boolean aligning;
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
		tracking = preSneaking = aligning = false;
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
			edge = sneaking = placed = preSneaking = aligning = false;
			unsneakTicks = 0;
			return;
		}

		float spread = randomize ? jitter : 0.0F;
		offset = edgeOffset / 100.0F * (1.0F - spread * 0.3F);
		edge = player.onGround && overEdge(player);
		if (!edge) placed = false;
		if (preSneak) updatePreSneak(player);
		boolean jump = sneakOnJump && !player.onGround && player.velocityY < 0.0D
			&& player.y - groundY <= jumpSneakHeight * (1.0F - spread * 0.3F)
			&& (!onlyWhileMoving || moving(player)) && landsOverEdge(player);
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
		InputState input = IllusionClient.INPUT.player;
		float forward = input.forwardAxis();
		double[] direction = MoveUtils.inputDirection(player.yaw, forward, forward == 0.0F ? input.sidewaysAxis() : 0.0F);
		double x = direction[0];
		double z = direction[1];

		double[] move = move(player);
		boolean edgeX = unsupported(player, move[0], 0.0D);
		boolean edgeZ = unsupported(player, 0.0D, move[1]);

		if (Math.min(Math.abs(x), Math.abs(z)) > DIAGONAL_RATIO * Math.hypot(x, z) && edgeX != edgeZ
			&& unsupported(player, edgeX ? 0.0D : Math.signum(x) * SECOND_EDGE_REACH, edgeX ? Math.signum(z) * SECOND_EDGE_REACH : 0.0D)) {
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

		if (rotating) {
			stillTicks = 0;
			turned += (float) Math.hypot(yawDelta, pitchDelta);
		} else if (++stillTicks >= STILL_TICKS) {
			turned = 0.0F;
		}
	}

	private void updatePreSneak(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		if (edge || !player.onGround || input.forward || input.jump || edgeDistance(player) >= preSneakDistance) {
			preSneaking = false;
			return;
		}
		if (!preSneaking) {
			preSneaking = turned >= MIN_TURN && player.pitch >= LOOKING_DOWN_PITCH
				&& angleFrom(player.yaw, BRIDGE_ANGLE, 0.0F) <= BRIDGE_ANGLE_TOLERANCE;
		}
	}

	private double edgeDistance(LocalClientPlayerEntity player) {
		int x = (int) Math.floor(player.x);
		int y = (int) Math.floor(player.y) - 1;
		int z = (int) Math.floor(player.z);
		double distance = Double.MAX_VALUE;
		if (open(x - 1, y, z)) distance = player.x - x;
		if (open(x + 1, y, z)) distance = Math.min(distance, x + 1 - player.x);
		if (open(x, y, z - 1)) distance = Math.min(distance, player.z - z);
		if (open(x, y, z + 1)) distance = Math.min(distance, z + 1 - player.z);
		return distance;
	}

	private boolean open(int x, int y, int z) {
		return minecraft.world.isAir(new BlockPos(x, y, z));
	}

	private void align(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		if (!input.back || input.forward || input.left || input.right
			|| angleFrom(player.yaw, 90.0F, 45.0F) > DIAGONAL_TOLERANCE) {
			aligning = false;
			return;
		}

		InputState client = IllusionClient.INPUT.client;
		double[] back = acceleration(player, -1.0F, 0.0F, client.sneak);
		double[] backLeft = acceleration(player, -1.0F, 1.0F, client.sneak);
		double slope = Math.signum(back[0]) * Math.signum(back[1]);
		double settle = 1.0D / (1.0D - (player.onGround ? GROUND_DRAG : AIR_DRAG));
		double line = player.x - slope * player.z + (player.velocityX + back[0] - slope * (player.velocityZ + back[1])) * settle;
		double error = line - Math.round(line);
		if (!aligning && Math.abs(error) <= ALIGN_START) return;

		double shift = (backLeft[0] - back[0] - slope * (backLeft[1] - back[1])) * settle;
		aligning = Math.abs(error) > Math.abs(shift) / 2.0D;
		if (!aligning) return;
		client.left = error * shift < 0.0D;
		client.right = error * shift > 0.0D;
	}

	private static float angleFrom(float yaw, float step, float target) {
		float angle = (yaw % step + step) % step;
		float distance = Math.abs(angle - target);
		return Math.min(distance, step - distance);
	}

	private static boolean moving(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.player;
		double speed = Math.hypot(player.velocityX, player.velocityZ);
		if (speed <= MOVING_SPEED) return !input.forward && (input.back || input.left || input.right);
		double[] facing = MoveUtils.inputDirection(player.yaw, 1.0F, 0.0F);
		return player.velocityX * facing[0] + player.velocityZ * facing[1] < speed * DIAGONAL_RATIO;
	}

	private boolean overEdge(LocalClientPlayerEntity player) {
		double[] move = move(player);
		return unsupported(player, move[0], move[1]);
	}

	private static double[] move(LocalClientPlayerEntity player) {
		InputState input = IllusionClient.INPUT.client;
		double[] acceleration = acceleration(player, input.forwardAxis(), input.sidewaysAxis(), false);
		return new double[]{player.velocityX + acceleration[0], player.velocityZ + acceleration[1]};
	}

	private static double[] acceleration(LocalClientPlayerEntity player, float forward, float sideways, boolean sneak) {
		float scale = sneak ? INPUT_SCALE * SNEAK_SCALE : INPUT_SCALE;
		forward *= scale;
		sideways *= scale;
		double speed = (player.onGround ? player.getSpeed() : AIR_SPEED) / Math.max(1.0D, Math.hypot(forward, sideways));
		double[] direction = MoveUtils.inputDirection(player.yaw, forward, sideways);
		return new double[]{direction[0] * speed, direction[1] * speed};
	}

	private boolean landsOverEdge(LocalClientPlayerEntity player) {
		double x = 0.0D, y = player.y, z = 0.0D;
		double velocityX = player.velocityX, velocityY = player.velocityY, velocityZ = player.velocityZ;
		for (int tick = 0; tick < MAX_FALL_TICKS && y > groundY; tick++) {
			x += velocityX;
			y += velocityY;
			z += velocityZ;
			velocityX *= AIR_DRAG;
			velocityZ *= AIR_DRAG;
			velocityY = (velocityY - GRAVITY) * VERTICAL_DRAG;
		}
		return unsupported(player, x, groundY - player.y, z);
	}

	private boolean unsupported(LocalClientPlayerEntity player, double x, double z) {
		return unsupported(player, x, 0.0D, z);
	}

	private boolean unsupported(LocalClientPlayerEntity player, double x, double y, double z) {
		double inset = player.width / 2.0F - offset;
		Box feet = player.getShape().contract(inset, 0.0D, inset).moved(x, y - 1.0D, z);
		return minecraft.world.getCollisions(player, feet).isEmpty();
	}
}
