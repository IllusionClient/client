package org.codeberg.illusionclient.event;

public final class UseItemEvent extends CancellableEvent {
	private static final UseItemEvent INSTANCE = new UseItemEvent();
	public boolean click;

	private UseItemEvent() {
	}

	public static UseItemEvent of(boolean click) {
		INSTANCE.cancelled = false;
		INSTANCE.click = click;
		return INSTANCE;
	}
}
