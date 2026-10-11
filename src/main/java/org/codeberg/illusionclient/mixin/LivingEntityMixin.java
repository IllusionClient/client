package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.living.LivingEntity;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.JumpDelayEvent;
import org.codeberg.illusionclient.event.JumpEvent;
import org.codeberg.illusionclient.event.JumpYawEvent;
import org.codeberg.illusionclient.util.PlayerUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
	@ModifyExpressionValue(method = "mobTick", at = @At(value = "CONSTANT", args = "intValue=10"))
	private int illusion$jumpDelay(int ticks) {
		if (!PlayerUtils.isLocalPlayer(this)) return ticks;

		JumpDelayEvent event = JumpDelayEvent.of(ticks);
		IllusionClient.BUS.publish(event);
		return event.ticks;
	}

	@ModifyExpressionValue(method = "mobTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/LivingEntity;onGround:Z", opcode = Opcodes.GETFIELD))
	private boolean illusion$jump(boolean onGround) {
		if (!onGround || !PlayerUtils.isLocalPlayer(this)) return onGround;

		JumpEvent event = JumpEvent.of();
		IllusionClient.BUS.publish(event);
		return !event.cancelled;
	}

	@ModifyExpressionValue(method = "jump",at = @At(value = "FIELD", target = "Lnet/minecraft/entity/living/LivingEntity;yaw:F", opcode = Opcodes.GETFIELD))
	private float illusion$jumpYaw(float yaw) {
		if (!PlayerUtils.isLocalPlayer(this)) return yaw;

		JumpYawEvent event = JumpYawEvent.of(yaw);
		IllusionClient.BUS.publish(event);
		return event.yaw;
	}
}
