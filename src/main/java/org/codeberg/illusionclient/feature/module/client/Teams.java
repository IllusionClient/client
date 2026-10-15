package org.codeberg.illusionclient.feature.module.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Formatting;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@Description("Decides which players count as teammates.")
@ModuleInfo(name = "Teams", category = Category.CLIENT)
public class Teams extends Module {
	@Description("Teammates share your nametag color.")
	@Check("Match own nametag")
	public boolean matchOwnNametag;

	@Description("Teammates have this nametag color.")
	@Combo(value = "Colors", options = {
		"Black", "Dark Blue", "Dark Green",
		"Dark Aqua", "Dark Red", "Dark Purple",
		"Gold", "Gray", "Dark Gray",
		"Blue", "Green", "Aqua",
		"Red", "Light Purple", "Yellow",
		"White", "None"
	})
	public String colors = "None";

	private String teamColorName;
	private Formatting teamColor;

	public static boolean isTeammate(PlayerEntity player) {
		Teams teams = IllusionClient.MODULES.get(Teams.class);
		if (!teams.isEnabled()) return false;

		Formatting teamColor = teams.teamColor();
		if (teamColor == null && !teams.matchOwnNametag) return false;

		Formatting color = nametagColor(player);
		if (color == null) return false;
		if (teams.matchOwnNametag && color == nametagColor(Minecraft.getInstance().player)) return true;

		return color == teamColor;
	}

	private Formatting teamColor() {
		if (!colors.equals(teamColorName)) {
			teamColorName = colors;
			teamColor = Formatting.byName(colors);
		}
		return teamColor;
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
