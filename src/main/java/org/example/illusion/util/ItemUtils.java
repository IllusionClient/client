package org.example.illusion.util;

import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public final class ItemUtils {
	public static boolean isBlock(ItemStack stack) {
		return stack != null && stack.getItem() instanceof BlockItem;
	}
}
