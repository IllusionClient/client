package org.codeberg.illusionclient.mixin;

import net.minecraft.client.entity.living.player.RemoteClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RemoteClientPlayerEntity.class)
public interface RemoteClientPlayerEntityAccessor {
	@Accessor("lerpSteps")
	int illusion$lerpSteps();

	@Accessor("lerpX")
	double illusion$lerpX();

	@Accessor("lerpY")
	double illusion$lerpY();

	@Accessor("lerpZ")
	double illusion$lerpZ();

	@Accessor("lerpYaw")
	double illusion$lerpYaw();

	@Accessor("lerpPitch")
	double illusion$lerpPitch();
}
