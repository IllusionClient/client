package org.codeberg.illusionclient.event;

public final class JumpEvent extends CancellableEvent {
	private static final JumpEvent INSTANCE = new JumpEvent();

	private JumpEvent() {
	}

	public static JumpEvent of() {
		INSTANCE.cancelled = false;
		return INSTANCE;
	}
}
