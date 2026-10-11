package org.codeberg.illusionclient.util;

import net.minecraft.network.handler.PacketHandler;
import net.minecraft.network.packet.Packet;

import java.util.ArrayDeque;
import java.util.Queue;

public final class PacketQueue {
	private final Queue<Held> packets = new ArrayDeque<>();

	public boolean isEmpty() {
		return packets.isEmpty();
	}

	public void add(Packet<PacketHandler> packet, PacketHandler listener) {
		packets.add(new Held(packet, listener, System.currentTimeMillis()));
	}

	public void clear() {
		packets.clear();
	}

	public void release(long minimumAge) {
		long cutoff = System.currentTimeMillis() - minimumAge;
		while (!packets.isEmpty() && packets.peek().time <= cutoff) {
			Held held = packets.poll();
			held.packet.handle(held.listener);
		}
	}

	private record Held(Packet<PacketHandler> packet, PacketHandler listener, long time) {
	}
}
