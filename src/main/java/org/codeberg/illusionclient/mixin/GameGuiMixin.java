package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.Window;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameGui.class)
public class GameGuiMixin {
	@Unique
	private Window illusion$window;

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/platform/GlStateManager;enableBlend()V",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void illusion$render2DPre(float tickDelta, CallbackInfo ci, @Local Window window) {
		illusion$window = window;
		IllusionClient.BUS.publish(Render2DEvent.Pre.of(tickDelta, window));
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void illusion$render2DPost(float tickDelta, CallbackInfo ci) {
		IllusionClient.BUS.publish(Render2DEvent.Post.of(tickDelta, illusion$window));
	}
}
