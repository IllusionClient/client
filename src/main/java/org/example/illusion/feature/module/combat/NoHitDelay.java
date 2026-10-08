package org.example.illusion.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import org.example.illusion.event.AttackEvent;
import org.example.illusion.event.MiningCooldownEvent;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;
import org.example.illusion.util.PlayerUtils;

@ModuleInfo(name = "No Hit Delay", category = Category.COMBAT)
@Description("Removes the attack cooldown that follows a missed swing.")
public class NoHitDelay extends Module {
	@Combo(value = "Mode", options = {"Regular", "Legit"})
	public String mode = "Regular";
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
