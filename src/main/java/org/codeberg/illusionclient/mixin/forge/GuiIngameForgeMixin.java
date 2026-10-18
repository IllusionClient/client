package org.codeberg.illusionclient.mixin.forge;

//? if forge {
/*import net.minecraft.client.render.Window;
import net.minecraftforge.client.GuiIngameForge;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiIngameForge.class)
public class GuiIngameForgeMixin {
	@Shadow(remap = false)
	private Window res;

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/render/platform/GlStateManager;enableBlend()V",
			shift = At.Shift.AFTER,
			ordinal = 0
		)
	)
	private void illusion$render2DPre(float tickDelta, CallbackInfo ci) {
		IllusionClient.BUS.publish(Render2DEvent.Pre.of(tickDelta, res));
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void illusion$render2DPost(float tickDelta, CallbackInfo ci) {
		IllusionClient.BUS.publish(Render2DEvent.Post.of(tickDelta, res));
	}
}
*///?}
