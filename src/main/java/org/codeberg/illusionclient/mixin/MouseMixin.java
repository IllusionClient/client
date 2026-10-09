package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.codeberg.illusionclient.util.MouseUtils;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Mouse.class, remap = false)
public class MouseMixin {
	@ModifyReturnValue(method = "next", at = @At("RETURN"))
	private static boolean illusion$next(boolean hasEvent) {
		return MouseUtils.next(hasEvent);
	}

	@ModifyReturnValue(method = "getEventButton", at = @At("RETURN"))
	private static int illusion$eventButton(int button) {
		return MouseUtils.synthetic() ? MouseUtils.eventButton() : button;
	}

	@ModifyReturnValue(method = "getEventButtonState", at = @At("RETURN"))
	private static boolean illusion$eventButtonState(boolean state) {
		return MouseUtils.synthetic() ? MouseUtils.eventState() : state;
	}

	@ModifyReturnValue(method = "getEventDWheel", at = @At("RETURN"))
	private static int illusion$eventDWheel(int wheel) {
		return MouseUtils.synthetic() ? 0 : wheel;
	}
}
