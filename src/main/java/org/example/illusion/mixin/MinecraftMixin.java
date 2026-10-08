package org.example.illusion.mixin;

import net.minecraft.client.Minecraft;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.*;
import org.lwjgl.input.Keyboard;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
	@Shadow
	private int attackCooldown;

	@Inject(method = "init", at = @At("TAIL"))
	private void illusion$init(CallbackInfo ci) {
		IllusionClient.init();
	}

	@Inject(method = "doAttack", at = @At("HEAD"))
	private void illusion$attack(CallbackInfo ci) {
		AttackEvent event = new AttackEvent();
		IllusionClient.BUS.publish(event);
		if (event.resetCooldown) attackCooldown = 0;
	}

	@Redirect(
		method = "handleMouseDown",
		at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;attackCooldown:I", opcode = Opcodes.GETFIELD)
	)
	private int illusion$miningCooldown(Minecraft minecraft) {
		MiningCooldownEvent event = new MiningCooldownEvent(attackCooldown);
		IllusionClient.BUS.publish(event);
		return event.cooldown;
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

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;handleInputs()V"))
	private void illusion$screenInput(CallbackInfo ci) {
		IllusionClient.BUS.publish(new ScreenInputEvent());
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
