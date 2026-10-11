package org.codeberg.illusionclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.network.handler.ClientPlayPacketHandler;
import net.minecraft.network.PacketUtils;
import net.minecraft.network.packet.s2c.play.KeepAliveS2CPacket;
import org.codeberg.illusionclient.feature.module.combat.Backtrack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Inject(method = "handleKeepAlive", at = @At("HEAD"))
	private void illusion$keepAliveOnClientThread(KeepAliveS2CPacket packet, CallbackInfo ci) {
		if (Backtrack.holdsKeepAlives()) {
			PacketUtils.ensureOnSameThread(packet, (ClientPlayPacketHandler) this, Minecraft.getInstance());
		}
	}
}
