package org.example.illusion.mixin;

import net.minecraft.client.render.GameRenderer;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.Render3DEvent;
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
}
