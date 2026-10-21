package org.codeberg.illusionclient.mixin;

import net.minecraft.client.TickTimer;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.TimerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TickTimer.class)
public class TickTimerMixin {
	@Shadow
	public int ticksThisFrame;

	@Shadow
	public float partialTick;

	@Inject(method = "advance", at = @At("TAIL"))
	private void illusion$advance(CallbackInfo ci) {
		TimerEvent event = TimerEvent.of(ticksThisFrame, partialTick);
		IllusionClient.BUS.publish(event);
		ticksThisFrame = event.ticks;
		partialTick = event.partialTick;
	}
}
