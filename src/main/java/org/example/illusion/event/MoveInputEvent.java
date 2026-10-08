package org.example.illusion.event;

public final class MoveInputEvent implements Event {
	private static final MoveInputEvent INSTANCE = new MoveInputEvent();
	public float forward;
	public float sideways;

	private MoveInputEvent() {
	}

	public static MoveInputEvent of(float forward, float sideways) {
		INSTANCE.forward = forward;
		INSTANCE.sideways = sideways;
		return INSTANCE;
	}
}
