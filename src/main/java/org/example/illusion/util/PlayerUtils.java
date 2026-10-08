package org.example.illusion.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.world.HitResult;

public class PlayerUtils {
	public static boolean aimingAtBlock() {
		HitResult target = Minecraft.getInstance().crosshairTarget;
		return target != null && target.type == HitResult.Type.BLOCK;
	}

	public static boolean aimingAtPlayer() {
		HitResult target = Minecraft.getInstance().crosshairTarget;
		return target != null && target.entity instanceof PlayerEntity;
	}
}
