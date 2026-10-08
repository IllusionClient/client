package org.example.illusion.mixin;

import net.minecraft.client.entity.living.player.Input;
import net.minecraft.client.entity.living.player.KeyboardInput;
import net.minecraft.client.options.KeyBinding;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.MoveInputEvent;
import org.example.illusion.event.SneakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends Input {
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;isPressed()Z", ordinal = 4))
	private void illusion$moveInput(CallbackInfo ci) {
		MoveInputEvent event = new MoveInputEvent(movementForward, movementSideways);
		IllusionClient.BUS.publish(event);
		movementForward = event.forward;
		movementSideways = event.sideways;
	}

	@Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;isPressed()Z", ordinal = 5))
	private boolean illusion$sneak(KeyBinding sneakKey) {
		SneakEvent event = new SneakEvent(sneakKey.isPressed());
		IllusionClient.BUS.publish(event);
		return event.sneaking;
	}
}
