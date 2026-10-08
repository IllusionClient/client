package org.example.illusion.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.Window;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.Render2DEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public class GameGuiMixin {
	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/platform/GlStateManager;enableBlend()V",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void illusion$render2D(float tickDelta, CallbackInfo ci, @Local Window window) {
		IllusionClient.BUS.publish(Render2DEvent.of(tickDelta, window));
	}
}
