package org.codeberg.illusionclient.feature.module.combat;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;

@Description("Limits Aim Assist and Auto Clicker to weapons.")
@ModuleInfo(name = "Weapons", category = Category.COMBAT)
public class Weapons extends Module {
	@Description("Counts swords as weapons.")
	@Check("Swords")
	public boolean swords = true;

	@Description("Counts axes as weapons.")
	@Check("Axes")
	public boolean axes = true;

	@Description("Counts sticks as weapons.")
	@Check("Sticks")
	public boolean sticks = false;

	@Description("Counts an empty hand as a weapon.")
	@Check("Fists")
	public boolean fists = false;

	@Description("Counts any item with Sharpness as a weapon.")
	@Check("Sharpness")
	public boolean sharpness = true;

	@Description("Counts any item with Knockback as a weapon.")
	@Check("Knockback")
	public boolean knockback = true;

	@Description("Counts any item with Fire Aspect as a weapon.")
	@Check("Fire Aspect")
	public boolean fireAspect = true;

	public static boolean isWeapons(ItemStack stack) {
		Weapons weapons = IllusionClient.MODULES.get(Weapons.class);
		if (!weapons.isEnabled()) return true;
		if (stack == null) return weapons.fists;

		Item item = stack.getItem();
		if (weapons.swords && item instanceof SwordItem) return true;
		if (weapons.axes && item instanceof AxeItem) return true;
		if (weapons.sticks && item == Items.STICK) return true;

		return weapons.sharpness && EnchantmentHelper.getLevel(Enchantment.SHARPNESS.id, stack) > 0
			|| weapons.knockback && EnchantmentHelper.getLevel(Enchantment.KNOCKBACK.id, stack) > 0
			|| weapons.fireAspect && EnchantmentHelper.getLevel(Enchantment.FIRE_ASPECT.id, stack) > 0;
	}
}
