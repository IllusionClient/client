package org.codeberg.illusionclient.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import org.codeberg.illusionclient.event.AttackEvent;
import org.codeberg.illusionclient.event.MiningCooldownEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.PlayerUtils;

@ModuleInfo(name = "No Hit Delay", category = Category.COMBAT)
@Description("Removes the cooldown after a missed swing.")
public class NoHitDelay extends Module {
	@Description("Regular always, Legit only while aiming at a player.")
	@Combo(value = "Mode", options = {"Regular", "Legit"})
	public String mode = "Regular";

	@Description("Removes the delay between mining blocks.")
	@Check("Remove Mining Penalty")
	public boolean removeMiningPenalty = false;

	@Override
	public String details() {
		return mode;
	}

	@Listen
	public void onAttack(AttackEvent event) {
		event.resetCooldown = !mode.equals("Legit") || PlayerUtils.aimingAtPlayer();
	}

	@Listen
	public void onMiningCooldown(MiningCooldownEvent event) {
		if (removeMiningPenalty) event.cooldown = 0;
	}
}
