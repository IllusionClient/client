package org.example.illusion.event;

public final class SneakEvent implements Event {
	private static final SneakEvent INSTANCE = new SneakEvent();
	public boolean sneaking;

	private SneakEvent() {
	}

	public static SneakEvent of(boolean sneaking) {
		INSTANCE.sneaking = sneaking;
		return INSTANCE;
	}
}
