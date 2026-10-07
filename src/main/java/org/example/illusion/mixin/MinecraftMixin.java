package org.example.illusion.mixin;

import net.minecraft.client.Minecraft;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.HitDelayEvent;
import org.example.illusion.event.KeyPressEvent;
import org.example.illusion.event.PlayerTickEvent;
import org.example.illusion.event.UseDelayEvent;
import org.example.illusion.event.UseItemEvent;
import org.example.illusion.event.WorldLoadEvent;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private void illusion$init(CallbackInfo ci) {
        IllusionClient.init();
    }

    @ModifyConstant(method = "doAttack", constant = @Constant(intValue = 10))
    private int illusion$hitDelay(int delay) {
        HitDelayEvent event = new HitDelayEvent();
        IllusionClient.BUS.publish(event);
        return event.delay;
    }

    @ModifyConstant(method = "doUse", constant = @Constant(intValue = 4))
    private int illusion$useDelay(int delay) {
        UseDelayEvent event = new UseDelayEvent();
        IllusionClient.BUS.publish(event);
        return event.delay;
    }

    @Inject(method = "doUse", at = @At("HEAD"), cancellable = true)
    private void illusion$useItem(CallbackInfo ci) {
        UseItemEvent event = new UseItemEvent();
        IllusionClient.BUS.publish(event);
        if (event.cancelled) ci.cancel();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;handleGuiKeyBindings()V"))
    private void illusion$keyPress(CallbackInfo ci) {
        IllusionClient.BUS.publish(new KeyPressEvent(Keyboard.getEventKey(), Keyboard.getEventKeyState()));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void illusion$tick(CallbackInfo ci) {
        IllusionClient.BUS.publish(new PlayerTickEvent());
    }

    @Inject(
            method = "setWorld(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/world/ClientWorld;addEntity(Lnet/minecraft/entity/Entity;)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void illusion$worldLoad(CallbackInfo ci) {
        IllusionClient.BUS.publish(new WorldLoadEvent());
    }
}
