package org.codeberg.illusionclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.TickTimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("timer")
	TickTimer illusion$timer();

	@Invoker("doAttack")
	void illusion$doAttack();
}
