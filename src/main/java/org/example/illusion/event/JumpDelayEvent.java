package org.example.illusion.event;

public final class JumpDelayEvent implements Event {
	private static final JumpDelayEvent INSTANCE = new JumpDelayEvent();
	public int ticks;

	private JumpDelayEvent() {
	}

	public static JumpDelayEvent of(int ticks) {
		INSTANCE.ticks = ticks;
		return INSTANCE;
	}
}
