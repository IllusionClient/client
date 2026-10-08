package org.example.illusion.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.example.illusion.feature.module.client.AntiBot;
import org.example.illusion.feature.module.client.Teams;

public class CombatUtils {
	public static boolean isTarget(Entity entity) {
		if (!(entity instanceof PlayerEntity player)) return false;

		return !Teams.isTeammate(player) && !AntiBot.isBot(player);
	}

	public static double distanceTo(Entity entity) {
		Vec3d eye = Minecraft.getInstance().player.getEyePosition(1.0F);
		Box box = entity.getShape();
		double x = eye.x - MathHelper.clamp(eye.x, box.minX, box.maxX);
		double y = eye.y - MathHelper.clamp(eye.y, box.minY, box.maxY);
		double z = eye.z - MathHelper.clamp(eye.z, box.minZ, box.maxZ);
		return Math.sqrt(x * x + y * y + z * z);
	}
}
