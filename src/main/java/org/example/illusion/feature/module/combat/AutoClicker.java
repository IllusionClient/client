package org.example.illusion.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.game.inventory.InventoryMenuScreen;
import net.minecraft.client.options.KeyBinding;
import org.example.illusion.event.PlayerTickEvent;
import org.example.illusion.event.Render3DEvent;
import org.example.illusion.event.ScreenInputEvent;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.feature.config.api.Slider;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;
import org.example.illusion.mixin.ScreenInvoker;
import org.example.illusion.util.PlayerUtils;
import org.lwjgl.input.Mouse;

import java.util.Random;

@ModuleInfo(name = "Auto Clicker", category = Category.COMBAT)
@Description("Clicks for you while the attack key is held.")
public class AutoClicker extends Module {
	private static final float MAX_FRAME_MS = 100.0F;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Random random = new Random();
	@Combo(value = "Click Pattern", options = {"Jitter", "Butterfly"})
	public String clickPattern = "Jitter";
	@Slider(value = "Target CPS", min = 1.0F, max = 25.0F)
	public float targetCps = 12.0F;
	@Check("Randomize")
	public boolean randomize = true;
	@Check("Simulate Exhaust")
	public boolean simulateExhaust = true;
	@Check("Allow Breaking Blocks")
	public boolean allowBreakingBlocks = true;
	@Check("Inventory")
	public boolean inventory = false;
	@Check("Randomize Inventory")
	public boolean randomizeInventorySpeed = true;
	@Check("Holding Weapon")
	public boolean holdingWeapon = false;
	@Check("Not Using Item")
	public boolean notUsingItem = false;
	private boolean clicking;
	private long lastFrame;
	private float wait;
	private int inventoryClicks;
	private int burstSize;
	private int burstLeft;
	private float burstSpent;
	private int gapStart;
	private int gapSpread;
	private float singleChance;
	private float tripleChance;
	private int streak;
	private float streakCps;
	private int phase;
	private float phaseCps;
	private int delayStart;
	private int delaySpread;
	private float pauseChance;
	private int exhaustTicks;
	private float exhaust;
	private String details;
	private String detailsPattern;
	private float detailsCps;

	@Override
	public String details() {
		if (details == null || targetCps != detailsCps || !clickPattern.equals(detailsPattern)) {
			detailsPattern = clickPattern;
			detailsCps = targetCps;
			details = clickPattern + " " + number(targetCps) + "cps";
		}
		return details;
	}

	@Override
	public void onDisable() {
		clicking = false;
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		updateRhythm();
	}

	@Listen
	public void onScreenInput(ScreenInputEvent event) {
		if (!activeInInventory()) {
			inventoryClicks = 0;
			return;
		}
		if (!randomizeInventorySpeed) inventoryClicks = 1;

		Screen screen = minecraft.screen;
		int x = Mouse.getX() * screen.width / minecraft.width;
		int y = screen.height - Mouse.getY() * screen.height / minecraft.height - 1;
		ScreenInvoker invoker = (ScreenInvoker) screen;
		for (; inventoryClicks > 0; inventoryClicks--) {
			invoker.illusion$mouseClicked(x, y, 0);
			invoker.illusion$mouseReleased(x, y, 0);
		}
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		long now = System.nanoTime();
		float delta = Math.min(MAX_FRAME_MS, (now - lastFrame) / 1.0E6F);
		lastFrame = now;

		LocalClientPlayerEntity player = minecraft.player;
		if (player == null || !(minecraft.screen == null ? activeInGame(player) : activeInInventory())) {
			clicking = false;
			return;
		}

		if (!clicking) {
			clicking = true;
			startBurst();
			wait = interval();
		}
		if (minecraft.screen != null && !randomizeInventorySpeed) return;

		for (wait -= delta; wait <= 0.0F; wait += interval()) click();
	}

	private boolean activeInGame(LocalClientPlayerEntity player) {
		if (!minecraft.options.attackKey.isPressed() || !player.isAlive()) return false;
		if (holdingWeapon && !Weapons.isWeapons(player.getItemInHand())) return false;
		if (notUsingItem && player.getItemInUse() != null) return false;
		return !allowBreakingBlocks || !PlayerUtils.aimingAtBlock();
	}

	private boolean activeInInventory() {
		return inventory && minecraft.screen instanceof InventoryMenuScreen
			&& Mouse.isButtonDown(0) && Screen.isShiftDown();
	}

	private void click() {
		if (minecraft.screen == null) {
			KeyBinding.click(minecraft.options.attackKey.getKeyCode());
		} else {
			inventoryClicks++;
		}
	}

	private void updateRhythm() {
		if (exhaustTicks > 0) {
			exhaustTicks--;
		} else if (random.nextInt(120) == 0) {
			exhaustTicks = 6 + random.nextInt(14);
			exhaust = 1.3F + random.nextFloat() * 0.4F;
		}
	}

	private float period() {
		if (!randomize) return 1000.0F / targetCps;

		float period = halfPeriod() + halfPeriod();
		if (random.nextFloat() < pauseChance) period += 70 + random.nextInt(45);
		return period;
	}

	private float halfPeriod() {
		if (streak-- <= 0) {
			if (phase-- <= 0) {
				phase = 5 + random.nextInt(15);
				phaseCps = random.nextFloat() * 2.0F - 1.0F;
				delayStart = 380 + random.nextInt(40);
				delaySpread = 50 + random.nextInt(70);
				pauseChance = 0.02F + random.nextFloat() * 0.05F;
				gapStart = 15 + random.nextInt(15);
				gapSpread = 15 + random.nextInt(20);
				singleChance = 0.08F + random.nextFloat() * 0.25F;
				tripleChance = random.nextFloat() * 0.08F;
			}
			streak = 4 + random.nextInt(30);
			streakCps = Math.max(1.0F, targetCps + phaseCps + random.nextInt(6) - 3);
		}
		return (delayStart + random.nextInt(delaySpread)) / streakCps;
	}

	private float interval() {
		float ms = period();
		if (simulateExhaust && exhaustTicks > 0) ms *= exhaust;
		if (!clickPattern.equals("Butterfly")) return ms;

		if (burstLeft > 0) {
			burstLeft--;
			float gap = randomize ? gapStart + random.nextFloat() * gapSpread : 30.0F;
			gap = Math.min(ms * 0.8F, gap);
			burstSpent += gap;
			return gap;
		}

		float rest = ms * burstSize - burstSpent;
		startBurst();
		return rest;
	}

	private void startBurst() {
		float roll = randomize ? random.nextFloat() : 1.0F;
		burstSize = roll < singleChance ? 1 : roll < singleChance + tripleChance ? 3 : 2;
		burstLeft = burstSize - 1;
		burstSpent = 0.0F;
	}
}
