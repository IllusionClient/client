package org.codeberg.illusionclient.event;

public final class UseDelayEvent implements Event {
	private static final UseDelayEvent INSTANCE = new UseDelayEvent();
	public int delay;

	private UseDelayEvent() {
	}

	public static UseDelayEvent of(int delay) {
		INSTANCE.delay = delay;
		return INSTANCE;
	}
}
