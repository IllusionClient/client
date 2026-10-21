package org.codeberg.illusionclient.feature.lag;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.handler.PacketHandler;
import net.minecraft.network.packet.Packet;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PacketReceiveEvent;
import org.codeberg.illusionclient.event.PlayerTickEvent;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class LagManager {
	private final Minecraft minecraft = Minecraft.getInstance();
	private final Queue<Held> packets = new ArrayDeque<>();
	private final List<Lag> lags = new CopyOnWriteArrayList<>();

	public LagManager() {
		IllusionClient.BUS.subscribe(this);
	}

	public Lag create() {
		return create(packet -> true);
	}

	public Lag create(Predicate<Packet<?>> filter) {
		Lag lag = new Lag(filter);
		lags.add(lag);
		return lag;
	}

	public boolean holdsKeepAlives() {
		for (Lag lag : lags) {
			if (lag.active && lag.holdKeepAlives) return true;
		}
		return false;
	}

	@Listen
	public void onPacketReceive(PacketReceiveEvent event) {
		if (packets.isEmpty() && !wanted(event.packet())) return;

		event.cancelled = true;
		packets.add(new Held(event.packet(), event.listener(), System.currentTimeMillis()));
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		flush();
	}

	private boolean wanted(Packet<?> packet) {
		for (Lag lag : lags) {
			if (lag.active && lag.filter.test(packet)) return true;
		}
		return false;
	}

	private boolean holds(Held held, long now) {
		for (Lag lag : lags) {
			if (lag.active && now - held.time < lag.delay && lag.filter.test(held.packet)) return true;
		}
		return false;
	}

	private void flush() {
		if (minecraft.world == null || minecraft.player == null) {
			packets.clear();
			return;
		}

		long now = System.currentTimeMillis();
		while (!packets.isEmpty() && !holds(packets.peek(), now)) {
			Held held = packets.poll();
			held.packet.handle(held.listener);
		}
	}

	private record Held(Packet<PacketHandler> packet, PacketHandler listener, long time) {
	}

	public final class Lag {
		private final Predicate<Packet<?>> filter;
		private volatile boolean active;
		private volatile boolean holdKeepAlives = true;
		private long delay;

		private Lag(Predicate<Packet<?>> filter) {
			this.filter = filter;
		}

		public boolean isActive() {
			return active;
		}

		public void holdKeepAlives(boolean hold) {
			holdKeepAlives = hold;
		}

		public void hold(long delay) {
			this.delay = delay;
			active = true;
		}

		public void release() {
			if (!active) return;
			active = false;
			flush();
		}
	}
}
