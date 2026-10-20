package org.codeberg.illusionclient.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.game.inventory.InventoryMenuScreen;
import net.minecraft.client.options.KeyBinding;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.ScreenInputEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.mixin.ScreenInvoker;
import org.codeberg.illusionclient.util.ClickPattern;
import org.codeberg.illusionclient.util.MouseUtils;
import org.codeberg.illusionclient.util.PlayerUtils;
import org.lwjgl.input.Mouse;

@ModuleInfo(name = "Auto Clicker", category = Category.COMBAT)
@Description("Clicks for you while the attack key is held.")
public class AutoClicker extends Module {
	private static final float MAX_FRAME_MS = 100.0F;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final ClickPattern pattern = new ClickPattern();

	@Description("Jitter clicks evenly, Butterfly in bursts of two.")
	@Combo(value = "Click Pattern", options = {"Jitter", "Butterfly"})
	public String clickPattern = "Jitter";

	@Description("How many clicks per second to aim for.")
	@Slider(value = "Target CPS", min = 1.0F, max = 25.0F)
	public float targetCps = 12.0F;

	@Description("Varies click timing to avoid a fixed rhythm.")
	@Check("Randomize")
	public boolean randomize = true;

	@Description("Sometimes slows down briefly, like a tired hand.")
	@Check("Simulate Exhaust")
	public boolean simulateExhaust = true;

	@Description("Pauses on blocks so you can mine.")
	@Check("Allow Breaking Blocks")
	public boolean allowBreakingBlocks = true;

	@Description("Clicks while Shift-clicking in inventories.")
	@Check("Inventory")
	public boolean inventory = false;

	@Description("Uses the click pattern in inventories, not max speed.")
	@Requires("inventory")
	@Check("Randomize Inventory")
	public boolean randomizeInventorySpeed = true;

	@Description("Pauses while you use an item.")
	@Check("Not Using Item")
	public boolean notUsingItem = false;

	private long lastFrame;
	private boolean inScreen;
	private int inventoryClicks;
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
		stop();
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		LocalClientPlayerEntity player = minecraft.player;
		if (player != null) pattern.tick(player.yaw, player.pitch);
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
			IllusionClient.STATS.autoClicks++;
		}
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		long now = System.nanoTime();
		float delta = Math.min(MAX_FRAME_MS, (now - lastFrame) / 1.0E6F);
		lastFrame = now;

		LocalClientPlayerEntity player = minecraft.player;
		boolean screen = minecraft.screen != null;
		if (player == null || screen != inScreen || !(screen ? activeInInventory() : activeInGame(player))) {
			inScreen = screen;
			stop();
			return;
		}
		if (screen && !randomizeInventorySpeed) return;

		int edges = pattern.advance(delta, targetCps, randomize, simulateExhaust, clickPattern.equals("Butterfly"));
		if (screen) {
			inventoryClicks += pattern.presses();
			return;
		}
		IllusionClient.STATS.autoClicks += pattern.presses();

		int key = minecraft.options.attackKey.getKeyCode();
		if (key < 0) {
			for (; edges > 0; edges--) MouseUtils.toggle(key + 100);
		} else {
			for (int presses = pattern.presses(); presses > 0; presses--) KeyBinding.click(key);
		}
	}

	private void stop() {
		pattern.stop();
		MouseUtils.resync();
	}

	private boolean activeInGame(LocalClientPlayerEntity player) {
		if (!IllusionClient.INPUT.held(minecraft.options.attackKey) || !player.isAlive()) return false;
		if (!Weapons.isWeapons(player.getItemInHand())) return false;
		if (notUsingItem && player.getItemInUse() != null) return false;
		return !allowBreakingBlocks || !PlayerUtils.aimingAtBlock();
	}

	private boolean activeInInventory() {
		return inventory && minecraft.screen instanceof InventoryMenuScreen
			&& Mouse.isButtonDown(0) && Screen.isShiftDown();
	}
}
