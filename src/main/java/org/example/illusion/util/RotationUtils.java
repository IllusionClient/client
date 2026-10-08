package org.example.illusion.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtils {
	public static float[] rotationTo(Vec3d point) {
		Vec3d eye = Minecraft.getInstance().player.getEyePosition(1.0F);
		double x = point.x - eye.x;
		double y = point.y - eye.y;
		double z = point.z - eye.z;
		return new float[]{
			(float) Math.toDegrees(Math.atan2(z, x)) - 90.0F,
			(float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)))
		};
	}

	public static double angleTo(Entity entity, float yaw, float pitch) {
		Box box = entity.getShape();
		float[] rotation = rotationTo(new Vec3d(
			(box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D));
		return Math.hypot(MathHelper.wrapDegrees(rotation[0] - yaw), rotation[1] - pitch);
	}
}
