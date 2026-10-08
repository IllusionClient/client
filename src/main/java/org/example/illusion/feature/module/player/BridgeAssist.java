package org.example.illusion.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import org.example.illusion.event.SneakEvent;
import org.example.illusion.event.UseItemEvent;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.feature.config.api.Slider;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;

import java.util.Random;

@ModuleInfo(name = "Bridge Assist", category = Category.PLAYER)
@Description("Sneaks for you when you are near the edge of a block.")
public class BridgeAssist extends Module {
	private static final float LOOKING_DOWN_PITCH = 45.0F;
	private static final double DIAGONAL_RATIO = Math.sin(Math.toRadians(22.5D));
	private static final double SECOND_EDGE_REACH = 0.2D;
	private static final float MS_PER_TICK = 50.0F;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Random random = new Random();
	@Slider(value = "Edge Offset", min = 0.0F, max = 30.0F)
	public float edgeOffset = 15.0F;
	@Slider(value = "Unsneak Delay", min = 50.0F, max = 500.0F)
	public float unsneakDelay = 100.0F;
	@Combo(value = "Select Blocks", options = {"No", "On depletion", "Always"})
	public String selectBlocks = "No";
	@Check("Randomize")
	public boolean randomize = false;
	@Check("Sneak On Jump")
	public boolean sneakOnJump = false;
	@Check("Avoid Double-Sneaking")
	public boolean avoidDoubleSneaking = false;
	@Check("Sneak Key Pressed")
	public boolean sneakKeyPressed = false;
	@Check("Holding Blocks")
	public boolean holdingBlocks = true;
	@Check("Looking Down")
	public boolean lookingDown = false;
	@Check("Not Moving Forward")
	public boolean notMovingForward = false;
	private boolean edge;
	private boolean placed;
	private float offset;
	private boolean sneaking;
	private int unsneakTicks;
	private float jitter;
	private int blockSlot = -1;

	private static boolean isBlock(ItemStack stack) {
		return stack != null && stack.getItem() instanceof BlockItem;
	}

	@Override
	public String details() {
		return number(unsneakDelay) + "ms";
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
		if (active && !isBlock(inventory.getSelectedItem())
			&& (selectBlocks.equals("Always") || depleted && selectBlocks.equals("On depletion"))) {
			for (int slot = 0; slot < PlayerInventory.getHotbarSize(); slot++) {
				if (!isBlock(inventory.items[slot])) continue;
				inventory.selectedSlot = slot;
				break;
			}
		}

		boolean holding = isBlock(inventory.getSelectedItem());
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
		Input input = player.input;
		double yaw = Math.toRadians(player.yaw);
		double x = input.movementSideways * Math.cos(yaw) - input.movementForward * Math.sin(yaw);
		double z = input.movementForward * Math.cos(yaw) + input.movementSideways * Math.sin(yaw);

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
		double yaw = Math.toRadians(player.yaw);
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		double x = player.velocityX + (input.movementSideways * cos - input.movementForward * sin) * acceleration;
		double z = player.velocityZ + (input.movementForward * cos + input.movementSideways * sin) * acceleration;
		return unsupported(player, x, z);
	}

	private boolean unsupported(LocalClientPlayerEntity player, double x, double z) {
		double inset = player.width / 2.0F - offset;
		Box feet = player.getShape().contract(inset, 0.0D, inset).moved(x, -1.0D, z);
		return minecraft.world.getCollisions(player, feet).isEmpty();
	}
}
