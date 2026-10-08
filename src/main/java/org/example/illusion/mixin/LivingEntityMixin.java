package org.example.illusion.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.LivingEntity;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.JumpDelayEvent;
import org.example.illusion.event.JumpYawEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyConstant(method = "mobTick", constant = @Constant(intValue = 10))
	private int illusion$jumpDelay(int ticks) {
		if ((Object) this != Minecraft.getInstance().player) return ticks;

		JumpDelayEvent event = new JumpDelayEvent();
		IllusionClient.BUS.publish(event);
		return event.ticks;
	}

	@Redirect(method = "jump", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/LivingEntity;yaw:F", opcode = Opcodes.GETFIELD))
	private float illusion$jumpYaw(LivingEntity entity) {
		if (entity != Minecraft.getInstance().player) return entity.yaw;

		JumpYawEvent event = new JumpYawEvent(entity.yaw);
		IllusionClient.BUS.publish(event);
		return event.yaw;
	}
}
