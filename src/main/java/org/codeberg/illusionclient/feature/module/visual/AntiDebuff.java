package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.entity.living.effect.StatusEffect;
import org.codeberg.illusionclient.event.EffectVisualEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Anti Debuff", category = Category.VISUAL)
@Description("Hides negative visual effects.")
public class AntiDebuff extends Module {
	@Check("Blindness")
	public boolean blindness = true;

	@Check("Nausea")
	public boolean nausea = true;

	@Listen
	public void onEffectVisual(EffectVisualEvent event) {
		if (blindness && event.effect == StatusEffect.BLINDNESS || nausea && event.effect == StatusEffect.NAUSEA) {
			event.visible = false;
		}
	}
}
