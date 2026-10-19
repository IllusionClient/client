package org.codeberg.illusionclient.util;

import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;

public final class MoveUtils {
	public static double[] inputDirection(LocalClientPlayerEntity player) {
		Input input = player.input;
		return inputDirection(player.yaw, input.movementForward, input.movementSideways);
	}

	public static double[] inputDirection(float yaw, float forward, float sideways) {
		double radians = Math.toRadians(yaw);
		double sin = Math.sin(radians);
		double cos = Math.cos(radians);
		return new double[]{
			sideways * cos - forward * sin,
			forward * cos + sideways * sin
		};
	}
}
