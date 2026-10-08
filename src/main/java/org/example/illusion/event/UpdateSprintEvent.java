package org.example.illusion.event;

public final class UpdateSprintEvent implements Event {
	public boolean sprinting;

	public UpdateSprintEvent(boolean sprinting) {
		this.sprinting = sprinting;
	}
}
