package org.codeberg.illusionclient.event;

public final class UpdateSprintEvent implements Event {
	private static final UpdateSprintEvent INSTANCE = new UpdateSprintEvent();
	public boolean sprinting;

	private UpdateSprintEvent() {
	}

	public static UpdateSprintEvent of(boolean sprinting) {
		INSTANCE.sprinting = sprinting;
		return INSTANCE;
	}
}
