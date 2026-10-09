package org.codeberg.illusionclient.event;

public final class PlayerTickEvent implements Event {
	public static final PlayerTickEvent INSTANCE = new PlayerTickEvent();

	private PlayerTickEvent() {
	}
}
