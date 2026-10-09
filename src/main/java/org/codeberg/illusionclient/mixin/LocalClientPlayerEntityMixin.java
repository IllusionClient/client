package org.codeberg.illusionclient.mixin;

import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.UpdateSprintEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalClientPlayerEntity.class)
public abstract class LocalClientPlayerEntityMixin extends Entity {

	public LocalClientPlayerEntityMixin(World world) {
		super(world);
	}

	@Inject(
		method = "mobTick",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/player/PlayerAbilities;canFly:Z", ordinal = 1,
			opcode = Opcodes.GETFIELD)
	)
	private void illusion$updateSprint(CallbackInfo ci) {
		UpdateSprintEvent event = UpdateSprintEvent.of(isSprinting());
		IllusionClient.BUS.publish(event);
		if (event.sprinting != isSprinting()) setSprinting(event.sprinting);
	}
}
