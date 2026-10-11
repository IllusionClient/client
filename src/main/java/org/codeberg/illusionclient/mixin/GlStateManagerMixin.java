package org.codeberg.illusionclient.mixin;

import net.minecraft.client.render.platform.GlStateManager;
import org.codeberg.illusionclient.util.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GlStateManager.class)
public class GlStateManagerMixin {
	@ModifyVariable(method = {"blendFunc", "blendFuncSeparate"}, at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private static int illusion$sourceFactor(int factor) {
		return RenderUtils.blendFactor(factor);
	}

	@ModifyVariable(method = {"blendFunc", "blendFuncSeparate"}, at = @At("HEAD"), ordinal = 1, argsOnly = true)
	private static int illusion$destinationFactor(int factor) {
		return RenderUtils.blendFactor(factor);
	}

	@Inject(method = "disableBlend", at = @At("HEAD"), cancellable = true)
	private static void illusion$keepBlend(CallbackInfo ci) {
		if (RenderUtils.constantAlpha()) ci.cancel();
	}
}
