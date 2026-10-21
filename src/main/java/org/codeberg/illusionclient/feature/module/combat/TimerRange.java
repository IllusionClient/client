package org.codeberg.illusionclient.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.world.HitResult;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.TimerEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.mixin.MinecraftAccessor;
import org.codeberg.illusionclient.util.CombatUtils;

@ModuleInfo(name = "Timer Range", category = Category.COMBAT)
@Description("Skips and pauses ticks to land the first hit in a trade.")
public class TimerRange extends Module {
	private static final float MILLIS_PER_TICK = 50.0F;
	private static final double REACH = 3.0D;
	private final Minecraft minecraft = Minecraft.getInstance();

	@Description("Regular skips then pauses, Legit pauses then skips like a lag spike.")
	@Combo(value = "Mode", options = {"Regular", "Legit"})
	public String mode = "Regular";

	@Description("How much time is skipped. Longer reaches farther but looks laggier.")
	@Slider(value = "Duration (ms)", min = 50.0F, max = 1000.0F)
	public float duration = 150.0F;

	@Description("Cooldown before skipping again.")
	@Slider(value = "Cooldown (s)", min = 0.0F, max = 5.0F)
	public float cooldown = 1.0F;

	@Description("Blocks with your sword while frozen to take less damage.")
	@Requires(value = "mode", is = "Regular")
	@Check("Block While Frozen")
	public boolean blockWhileFrozen = false;

	@Description("Only activates while the attack key is held.")
	@Check("Left Mouse Pressed")
	public boolean leftMousePressed = true;

	@Description("Only activates while holding a weapon.")
	@Check("Weapons Only")
	public boolean weaponsOnly = true;

	@Description("Pauses while you use an item.")
	@Check("Not Using Item")
	public boolean notUsingItem = true;

	private PlayerEntity target;
	private double lastDistance;
	private int queuedSkip;
	private int frozenTicks;
	private boolean bursting;
	private int burstBase;
	private int burstTicks;
	private boolean hit;
	private float heldPartialTick;
	private long readyAt;

	@Override
	public String details() {
		return mode + " " + number(duration) + " ms";
	}

	@Override
	public void onDisable() {
		reset();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		reset();
	}

	@Listen
	public void onTimer(TimerEvent event) {
		if (frozenTicks > 0) {
			int swallowed = Math.min(event.ticks, frozenTicks);
			frozenTicks -= swallowed;
			event.ticks -= swallowed;
			if (frozenTicks > 0 || event.ticks == 0) event.partialTick = heldPartialTick;
			if (frozenTicks == 0 && queuedSkip == 0) finish();
		}

		if (frozenTicks == 0 && queuedSkip > 0) {
			bursting = true;
			burstBase = event.ticks;
			burstTicks = 0;
			hit = false;
			event.ticks += queuedSkip;
			queuedSkip = 0;
		}
		heldPartialTick = event.partialTick;
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		LocalClientPlayerEntity player = minecraft.player;
		if (minecraft.world == null || player == null) {
			reset();
			return;
		}

		if (bursting) {
			burst(player);
			return;
		}
		if (frozenTicks > 0 || queuedSkip > 0) return;

		PlayerEntity nearest = findTarget(player);
		double distance = nearest == null ? 0.0D : CombatUtils.distanceTo(nearest);
		double closing = nearest == target ? lastDistance - distance : 0.0D;
		target = nearest;
		lastDistance = distance;

		if (target == null || System.currentTimeMillis() < readyAt || !conditionsMet(player)) return;
		int ticks = durationTicks();
		if (distance <= REACH || closing <= 0.0D || distance - closing * ticks > REACH) return;

		if (mode.equals("Legit")) frozenTicks = ticks;
		queuedSkip = ticks;
	}

	private void burst(LocalClientPlayerEntity player) {
		burstTicks++;
		if (!hit && target != null && player.getItemInUse() == null) {
			minecraft.gameRenderer.pick(1.0F);
			HitResult crosshair = minecraft.crosshairTarget;
			if (crosshair != null && crosshair.entity == target) {
				((MinecraftAccessor) minecraft).illusion$doAttack();
				hit = true;
				if (mode.equals("Regular")) ((MinecraftAccessor) minecraft).illusion$timer().ticksThisFrame = Math.max(burstTicks, burstBase);
			}
		}
		if (burstTicks < ((MinecraftAccessor) minecraft).illusion$timer().ticksThisFrame) return;

		bursting = false;
		if (mode.equals("Legit")) {
			finish();
			return;
		}

		frozenTicks = Math.max(0, burstTicks - burstBase);
		if (frozenTicks == 0) {
			finish();
			return;
		}
		ItemStack stack = player.getItemInHand();
		if (blockWhileFrozen && stack != null && stack.getItem() instanceof SwordItem && player.getItemInUse() == null) {
			minecraft.interactionManager.useItem(player, minecraft.world, stack);
		}
	}

	private void finish() {
		readyAt = System.currentTimeMillis() + (long) (cooldown * 1000.0F);
		target = null;
	}

	private void reset() {
		target = null;
		queuedSkip = 0;
		frozenTicks = 0;
		bursting = false;
	}

	private int durationTicks() {
		return Math.max(1, Math.round(duration / MILLIS_PER_TICK));
	}

	private boolean conditionsMet(LocalClientPlayerEntity player) {
		if (minecraft.screen != null || !player.isAlive()) return false;
		if (leftMousePressed && !IllusionClient.INPUT.held(minecraft.options.attackKey)) return false;
		if (weaponsOnly && !Weapons.isWeapons(player.getItemInHand())) return false;
		return !notUsingItem || player.getItemInUse() == null;
	}

	private PlayerEntity findTarget(LocalClientPlayerEntity self) {
		PlayerEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (PlayerEntity player : minecraft.world.players) {
			if (player == self || !player.isAlive() || !CombatUtils.isTarget(player)) continue;

			double distance = CombatUtils.distanceTo(player);
			if (distance < bestDistance) {
				best = player;
				bestDistance = distance;
			}
		}
		return best;
	}
}
