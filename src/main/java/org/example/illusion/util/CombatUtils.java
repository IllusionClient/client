package org.example.illusion.util;

import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import org.example.illusion.feature.module.client.AntiBot;
import org.example.illusion.feature.module.client.Teams;

public class CombatUtils {
	public static boolean isTarget(Entity entity) {
		if (!(entity instanceof PlayerEntity player)) return false;

		return !Teams.isTeammate(player) && !AntiBot.isBot(player);
	}
}
