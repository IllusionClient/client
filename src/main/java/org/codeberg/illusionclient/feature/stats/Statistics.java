package org.codeberg.illusionclient.feature.stats;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.world.HitResult;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.AttackEvent;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.client.ClickGuiModule;

public final class Statistics {
	private static final int COMBO_WINDOW = 10;

	private final long start = System.nanoTime();
	public final Object2LongMap<String> toggles = new Object2LongOpenHashMap<>();
	public long previousPlaytime;
	public long autoClicks;
	public long bestCombo;

	private long combo;
	private LivingEntity target;
	private int targetDamaged, playerDamaged, sinceAttack = Integer.MAX_VALUE;

	private static boolean tracked(Module module) {
		return !(module instanceof ClickGuiModule);
	}

	public long playtime() {
		return previousPlaytime + (System.nanoTime() - start) / 1_000_000L;
	}

	public void toggled(Module module) {
		if (tracked(module)) toggles.mergeLong(module.key, 1, Long::sum);
	}

	public Module mostToggled() {
		Module best = null;
		long max = 0;
		for (Module module : IllusionClient.MODULES.modules) {
			long count = toggles.getLong(module.key);
			if (count > max) {
				max = count;
				best = module;
			}
		}
		return best;
	}

	public int ping() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientPlayNetworkHandler handler = minecraft.getNetworkHandler();
		if (handler == null || minecraft.player == null) return -1;

		PlayerInfo info = handler.getOnlinePlayer(minecraft.player.getUuid());
		return info == null ? -1 : info.getPing();
	}

	@Listen
	public void onAttack(AttackEvent event) {
		HitResult hit = Minecraft.getInstance().crosshairTarget;
		if (hit == null || hit.type != HitResult.Type.ENTITY || !(hit.entity instanceof LivingEntity living)) return;

		if (living != target) {
			target = living;
			targetDamaged = living.damagedTimer;
		}
		sinceAttack = 0;
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.world == null || minecraft.player == null) {
			combo = 0;
			target = null;
			return;
		}

		int damaged = minecraft.player.damagedTimer;
		if (damaged > playerDamaged) combo = 0;
		playerDamaged = damaged;

		// The server only sends the hurt animation when a hit lands, so a rising hurt timer on the
		// entity we just attacked is the best hit confirmation the client gets.
		if (target == null) return;
		if (target.damagedTimer > targetDamaged && sinceAttack <= COMBO_WINDOW) bestCombo = Math.max(bestCombo, ++combo);
		targetDamaged = target.damagedTimer;
		if (sinceAttack < Integer.MAX_VALUE) sinceAttack++;
		if (target.removed) target = null;
	}
}
