package org.example.illusion.event;

public final class PlayerTickEvent implements Event {
	public static final PlayerTickEvent INSTANCE = new PlayerTickEvent();

	private PlayerTickEvent() {
	}
}
