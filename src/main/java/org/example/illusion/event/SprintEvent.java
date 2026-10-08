package org.example.illusion.event;

public final class SprintEvent implements Event {
	public boolean sprinting;

	public SprintEvent(boolean sprinting) {
		this.sprinting = sprinting;
	}
}
