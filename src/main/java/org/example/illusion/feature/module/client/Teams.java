package org.example.illusion.feature.module.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Formatting;
import org.example.illusion.IllusionClient;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;

@Description("Configures which players are considered friendly.")
@ModuleInfo(name = "Teams", category = Category.CLIENT)
public class Teams extends Module {
	@Check("Match own nametag")
	public boolean matchOwnNametag;

	@Combo(value = "Colors", options = {
		"Black", "Dark Blue", "Dark Green",
		"Dark Aqua", "Dark Red", "Dark Purple",
		"Gold", "Gray", "Dark Gray",
		"Blue", "Green", "Aqua",
		"Red", "Light Purple", "Yellow",
		"White", "None"
	})
	public String colors = "None";

	public static boolean isTeammate(PlayerEntity player) {
		Teams teams = IllusionClient.MODULES.get(Teams.class);
		if (!teams.isEnabled()) return false;

		Formatting color = nametagColor(player);
		if (color == null) return false;
		if (teams.matchOwnNametag && color == nametagColor(Minecraft.getInstance().player)) return true;

		return color == Formatting.byName(teams.colors);
	}

	private static Formatting nametagColor(PlayerEntity player) {
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

	@Override
	public String details() {
		return colors.equals("None") ? null : colors;
	}
}
