package org.codeberg.illusionclient.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;
import net.minecraft.world.HitResult;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Auto Tool", category = Category.PLAYER)
@Description("Switches to the best tool when you mine a block.")
public class AutoTool extends Module {
	private static final float MS_PER_TICK = 50.0F;
	private static final float HAND_SPEED = 1.0F;
	private final Minecraft minecraft = Minecraft.getInstance();

	@Description("How long to mine before switching.")
	@Slider(value = "Activation Time", min = 0.0F, max = 500.0F)
	public float activationTime = 150.0F;

	@Description("Returns to your previous slot when you stop mining.")
	@Check("Switch Back")
	public boolean switchBack = true;

	@Description("Works while holding a sword.")
	@Check("Sword")
	public boolean sword = true;

	@Description("Works while holding a tool.")
	@Check("Tool")
	public boolean tool = true;

	@Description("Works while your hand is empty.")
	@Check("Fists")
	public boolean fists = true;

	@Description("Works while holding any other item.")
	@Check("Other")
	public boolean other = true;

	@Description("Only works while you hold sneak.")
	@Check("Sneaking")
	public boolean sneaking = false;

	private int countdown;
	private int previousSlot = -1;
	private int toolSlot = -1;

	@Override
	public void onEnable() {
		countdown = Math.round(activationTime / MS_PER_TICK);
	}

	@Override
	public void onDisable() {
		release();
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		LocalClientPlayerEntity player = minecraft.player;
		if (player == null || minecraft.screen != null) return;
		PlayerInventory inventory = player.inventory;

		if (toolSlot != -1 && inventory.selectedSlot != toolSlot) {
			previousSlot = toolSlot = -1;
			IllusionClient.MODULES.releaseSlot(this);
			return;
		}
		if (toolSlot == -1 && !allowed(inventory.getSelectedItem())) return;

		Block block = target(player);
		if (block == null) {
			countdown = Math.round(activationTime / MS_PER_TICK);
			release();
			return;
		}
		if (countdown > 0) {
			countdown--;
			release();
			return;
		}

		int current = inventory.selectedSlot;
		int best = -1;
		float bestSpeed = HAND_SPEED;
		for (int i = 0; i < PlayerInventory.getHotbarSize(); i++) {
			int slot = (current + i) % PlayerInventory.getHotbarSize();
			ItemStack stack = inventory.items[slot];
			float speed = stack == null ? HAND_SPEED : stack.getMiningSpeed(block);
			if (speed <= bestSpeed) continue;
			best = slot;
			bestSpeed = speed;
		}
		if (best == -1 || best == current || !IllusionClient.MODULES.claimSlot(this)) return;

		if (previousSlot == -1) previousSlot = current;
		inventory.selectedSlot = toolSlot = best;
	}

	private Block target(LocalClientPlayerEntity player) {
		if (!IllusionClient.INPUT.held(minecraft.options.attackKey) || player.getItemInUse() != null) return null;
		if (sneaking && !IllusionClient.INPUT.player.sneak) return null;

		HitResult target = minecraft.crosshairTarget;
		if (target == null || target.type != HitResult.Type.BLOCK) return null;
		return minecraft.world.getBlockState(target.getPos()).getBlock();
	}

	private boolean allowed(ItemStack stack) {
		if (stack == null) return fists;
		Item item = stack.getItem();
		if (item instanceof SwordItem) return sword;
		if (item instanceof ToolItem || item instanceof ShearsItem) return tool;
		return other;
	}

	private void release() {
		IllusionClient.MODULES.releaseSlot(this);
		if (switchBack && previousSlot != -1 && minecraft.player != null) minecraft.player.inventory.selectedSlot = previousSlot;
		previousSlot = toolSlot = -1;
	}
}
