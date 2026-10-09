package org.codeberg.illusionclient.mixin;

import net.minecraft.client.render.GameRenderer;
import org.codeberg.illusionclient.IllusionClient;
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
}
