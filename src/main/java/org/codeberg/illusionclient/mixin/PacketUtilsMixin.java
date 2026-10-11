package org.codeberg.illusionclient.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.network.handler.PacketHandler;
import net.minecraft.network.packet.Packet;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PacketReceiveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketUtils$48215440")
public class PacketUtilsMixin {
	@WrapWithCondition(
		method = "run",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/Packet;handle(Lnet/minecraft/network/handler/PacketHandler;)V")
	)
	private boolean illusion$packetReceive(Packet<PacketHandler> packet, PacketHandler listener) {
		if (!(listener instanceof ClientPlayNetworkHandler)) return true;

		PacketReceiveEvent event = PacketReceiveEvent.of(packet, listener);
		IllusionClient.BUS.publish(event);
		return !event.cancelled;
	}
}
