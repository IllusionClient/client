package org.codeberg.illusionclient.feature.module.client;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@Description("Defines which players are considered bots.")
@ModuleInfo(name = "Anti Bot", category = Category.CLIENT)
public class AntiBot extends Module {
	private static final int MIN_AGE_TICKS = 20;
	private final Set<PlayerEntity> seenVisible = Collections.newSetFromMap(new WeakHashMap<>());
	private final Set<PlayerEntity> seenMoving = Collections.newSetFromMap(new WeakHashMap<>());
	/**
	 * Filter players with an invalid UUID.
	 */
	@Check("Invalid UUID")
	public boolean invalidUuid = true;
	/**
	 * Filter players that have been invisible from the moment they first spawned in.
	 */
	@Check("Always Invisible")
	public boolean alwaysInvisible = true;
	/**
	 * Filter players who have never moved (horizontally) at all.
	 */
	@Check("Always stationary")
	public boolean alwaysStationary = true;
	/**
	 * Filter players who are sleeping.
	 */
	@Check("Sleeping")
	public boolean sleeping = true;
	/**
	 * Filter players that spawned recently.
	 */
	@Check("Entity age")
	public boolean entityAge = true;

	public static boolean isBot(PlayerEntity player) {
		AntiBot antiBot = IllusionClient.MODULES.get(AntiBot.class);
		if (!antiBot.isEnabled() || player == Minecraft.getInstance().player) return false;

		if (antiBot.invalidUuid && !hasPlayerUuid(player)) return true;
		if (antiBot.alwaysInvisible && !antiBot.seenVisible.contains(player)) return true;
		if (antiBot.alwaysStationary && !antiBot.seenMoving.contains(player)) return true;
		if (antiBot.sleeping && player.isSleeping()) return true;

		return antiBot.entityAge && player.ticks < MIN_AGE_TICKS;
	}

	private static boolean hasPlayerUuid(PlayerEntity player) {
		int version = player.getUuid().version();
		return version == 4 || version == 3;
	}

	@Override
	public void onDisable() {
		seenVisible.clear();
		seenMoving.clear();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		seenVisible.clear();
		seenMoving.clear();
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.world == null) return;

		for (PlayerEntity player : minecraft.world.players) {
			if (!player.isInvisible()) seenVisible.add(player);
			if (player.x != player.prevX || player.z != player.prevZ) seenMoving.add(player);
		}
	}
}
