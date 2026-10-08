package org.example.illusion.util;

import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;

public class MoveUtils {
	public static double[] inputDirection(LocalClientPlayerEntity player) {
		Input input = player.input;
		double yaw = Math.toRadians(player.yaw);
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new double[]{
			input.movementSideways * cos - input.movementForward * sin,
			input.movementForward * cos + input.movementSideways * sin
		};
	}
}
