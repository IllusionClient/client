package org.codeberg.illusionclient.event;

public final class UseItemEvent extends CancellableEvent {
	private static final UseItemEvent INSTANCE = new UseItemEvent();

	private UseItemEvent() {
	}

	public static UseItemEvent of() {
		INSTANCE.cancelled = false;
		return INSTANCE;
	}
}
