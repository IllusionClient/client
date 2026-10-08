package org.example.illusion.feature.module.movement;

import io.github.nevalackin.radbus.Listen;
import org.example.illusion.event.JumpDelayEvent;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;

@ModuleInfo(name = "No Jump Delay", category = Category.MOVEMENT)
@Description("Removes the jump cooldown while holding the jump key.")
public class NoJumpDelay extends Module {
	@Listen
	public void onJumpDelay(JumpDelayEvent event) {
		event.ticks = 0;
	}
}
