package org.example.illusion.event;

public final class UseDelayEvent implements Event {
	public int delay;

	public UseDelayEvent(int delay) {
		this.delay = delay;
	}
}
