package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.living.LivingEntity;
import net.minecraft.entity.living.effect.StatusEffect;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.EffectVisualEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Inject(method = "render(IFJ)V", at = @At(value = "CONSTANT", args = "stringValue=hand"))
	private void illusion$render3D(int pass, float tickDelta, long renderTimeLimit, CallbackInfo ci) {
		IllusionClient.BUS.publish(Render3DEvent.of(tickDelta));
	}

	@WrapOperation(
		method = {"setupClearColor", "setupFog"},
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/living/LivingEntity;hasStatusEffect(Lnet/minecraft/entity/living/effect/StatusEffect;)Z")
	)
	private boolean illusion$effectVisual(LivingEntity entity, StatusEffect effect, Operation<Boolean> original) {
		EffectVisualEvent event = EffectVisualEvent.of(effect, original.call(entity, effect));
		IllusionClient.BUS.publish(event);
		return event.visible;
	}
}
