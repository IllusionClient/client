package org.example.illusion.mixin;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.HurtShakeEvent;
import org.example.illusion.event.RayTraceEntityEvent;
import org.example.illusion.event.Render3DEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Unique
	private RayTraceEntityEvent illusion$rayTrace;

	@Inject(method = "applyHurtCam", at = @At("HEAD"), cancellable = true)
	private void illusion$hurtShake(float tickDelta, CallbackInfo ci) {
		HurtShakeEvent event = new HurtShakeEvent();
		IllusionClient.BUS.publish(event);
		if (event.cancelled) ci.cancel();
	}

	@Inject(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getEyePosition(F)Lnet/minecraft/util/math/Vec3d;", shift = At.Shift.AFTER))
	private void illusion$rayTrace(float tickDelta, CallbackInfo ci) {
		IllusionClient.BUS.publish(illusion$rayTrace = new RayTraceEntityEvent());
	}

	@Redirect(method = "pick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPickRadius()F"))
	private float illusion$pickRadius(Entity entity) {
		return entity.getPickRadius() * illusion$rayTrace.borderMultiplier;
	}

	@ModifyConstant(method = "pick", constant = @Constant(doubleValue = 3.0D))
	private double illusion$reach(double reach) {
		return illusion$rayTrace.reach;
	}

	@Inject(method = "render(IFJ)V", at = @At(value = "CONSTANT", args = "stringValue=hand"))
	private void illusion$render3D(int pass, float tickDelta, long renderTimeLimit, CallbackInfo ci) {
		IllusionClient.BUS.publish(new Render3DEvent(tickDelta));
	}
}
