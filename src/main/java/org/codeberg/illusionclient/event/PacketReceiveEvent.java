package org.codeberg.illusionclient.event;

import net.minecraft.network.handler.PacketHandler;
import net.minecraft.network.packet.Packet;

public final class PacketReceiveEvent extends CancellableEvent {
	private static final PacketReceiveEvent INSTANCE = new PacketReceiveEvent();
	private Packet<PacketHandler> packet;
	private PacketHandler listener;

	private PacketReceiveEvent() {
	}

	public static PacketReceiveEvent of(Packet<PacketHandler> packet, PacketHandler listener) {
		INSTANCE.cancelled = false;
		INSTANCE.packet = packet;
		INSTANCE.listener = listener;
		return INSTANCE;
	}

	public Packet<PacketHandler> packet() {
		return packet;
	}

	public PacketHandler listener() {
		return listener;
	}
}
