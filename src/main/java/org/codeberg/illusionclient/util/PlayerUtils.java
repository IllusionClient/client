package org.codeberg.illusionclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Formatting;
import net.minecraft.world.HitResult;

public final class PlayerUtils {
	public static boolean aimingAtBlock() {
		HitResult target = Minecraft.getInstance().crosshairTarget;
		return target != null && target.type == HitResult.Type.BLOCK;
	}

	public static boolean aimingAtPlayer() {
		HitResult target = Minecraft.getInstance().crosshairTarget;
		return target != null && target.entity instanceof PlayerEntity;
	}

	public static boolean isLocalPlayer(Object entity) {
		return entity == Minecraft.getInstance().player;
	}

	public static Formatting nametagColor(PlayerEntity player) {
		String display = player.getDisplayName().getFormattedString();
		int nameStart = display.indexOf(player.getName());

		Formatting color = null;
		for (int i = 0; i + 1 < nameStart; i++) {
			if (display.charAt(i) != '§') continue;

			char code = Character.toLowerCase(display.charAt(++i));
			int id = Character.digit(code, 16);
			if (id >= 0) color = Formatting.byId(id);
			else if (code == 'r') color = null;
		}
		return color;
	}
}
