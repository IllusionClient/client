package org.example.illusion.event;

public final class JumpDelayEvent implements Event {
	public int ticks;

	public JumpDelayEvent(int ticks) {
		this.ticks = ticks;
	}
}
