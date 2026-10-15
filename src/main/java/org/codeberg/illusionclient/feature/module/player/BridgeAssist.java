package org.codeberg.illusionclient.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.util.math.Box;
import org.codeberg.illusionclient.event.SneakEvent;
import org.codeberg.illusionclient.event.UseItemEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Slider;
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

	@Description("Also sneaks while you jump.")
	@Check("Sneak On Jump")
	public boolean sneakOnJump = false;

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
	}

	@Listen
	public void onSneak(SneakEvent event) {
		LocalClientPlayerEntity player = minecraft.player;
		PlayerInventory inventory = player.inventory;
		boolean active = active(player, event.sneaking);

		boolean depleted = inventory.getSelectedItem() == null && inventory.selectedSlot == blockSlot;
		if (active && !ItemUtils.isBlock(inventory.getSelectedItem())
			&& (selectBlocks.equals("Always") || depleted && selectBlocks.equals("On depletion"))) {
			for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
				if (!ItemUtils.isBlock(inventory.items[slot])) continue;
				inventory.selectedSlot = slot;
				break;
			}
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
		boolean jump = sneakOnJump && (player.input.jumping || !player.onGround);
		if (edge || jump) unsneakTicks = Math.max(1, Math.round(unsneakDelay * (1.0F + spread * 0.5F) / MS_PER_TICK));

		boolean sneak = edge || jump || unsneakTicks-- > 0;
		if (sneaking && !sneak) jitter = random.nextFloat();
		sneaking = sneak;
		event.sneaking = sneak || !sneakKeyPressed && event.sneaking;
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

	private boolean active(LocalClientPlayerEntity player, boolean sneakKey) {
		if (minecraft.screen != null) return false;
		if (sneakKeyPressed && !sneakKey) return false;
		if (lookingDown && player.pitch < LOOKING_DOWN_PITCH) return false;
		return !notMovingForward || player.input.movementForward <= 0.0F;
	}

	private boolean overEdge(LocalClientPlayerEntity player) {
		Input input = player.input;
		double acceleration = player.getSpeed() / Math.max(1.0D, Math.hypot(input.movementForward, input.movementSideways));
		double[] direction = MoveUtils.inputDirection(player);
		return unsupported(player, player.velocityX + direction[0] * acceleration, player.velocityZ + direction[1] * acceleration);
	}

	private boolean unsupported(LocalClientPlayerEntity player, double x, double z) {
		double inset = player.width / 2.0F - offset;
		Box feet = player.getShape().contract(inset, 0.0D, inset).moved(x, -1.0D, z);
		return minecraft.world.getCollisions(player, feet).isEmpty();
	}
}
