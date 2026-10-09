package org.codeberg.illusionclient.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import org.codeberg.illusionclient.event.JumpDelayEvent;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@ModuleInfo(name = "No Jump Delay", category = Category.MOVEMENT)
@Description("Removes the jump cooldown while holding the jump key.")
public class NoJumpDelay extends Module {
	@Listen
	public void onJumpDelay(JumpDelayEvent event) {
		event.ticks = 0;
	}
}
