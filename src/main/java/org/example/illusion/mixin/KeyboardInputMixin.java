package org.example.illusion.mixin;

import net.minecraft.client.entity.living.player.KeyboardInput;
import net.minecraft.client.options.KeyBinding;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.SneakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/options/KeyBinding;isPressed()Z", ordinal = 5))
    private boolean illusion$sneak(KeyBinding sneakKey) {
        SneakEvent event = new SneakEvent(sneakKey.isPressed());
        IllusionClient.BUS.publish(event);
        return event.sneaking;
    }
}
