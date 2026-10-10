package org.codeberg.illusionclient.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "org.codeberg.chromatic.freelook.handler.FreelookHandler", remap = false)
public class FreelookHandlerMixin {
	@Dynamic
	@ModifyExpressionValue(method = "tick", at = @At(value = "FIELD", target = "Lorg/codeberg/chromatic/freelook/handler/FreelookHandler;enabledServer:Z"))
	private boolean illusion$enabledServer(boolean enabled) {
		return true;
	}

	@Dynamic
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lorg/polyfrost/oneconfig/api/hypixel/v1/HypixelUtils;isHypixel()Z"))
	private boolean illusion$isHypixel(boolean hypixel) {
		return false;
	}
}
