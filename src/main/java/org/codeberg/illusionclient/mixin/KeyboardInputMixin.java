package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.entity.living.player.KeyboardInput;
import net.minecraft.client.options.KeyBinding;
import org.codeberg.illusionclient.IllusionClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("HEAD"))
	private void illusion$updateInput(CallbackInfo ci) {
		IllusionClient.INPUT.update();
	}

	@WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;isPressed()Z"))
	private boolean illusion$clientInput(KeyBinding key, Operation<Boolean> original) {
		return IllusionClient.INPUT.resolve(key, original.call(key));
	}
}
