package org.example.illusion.event;

public final class MoveInputEvent implements Event {
	public float forward;
	public float sideways;

	public MoveInputEvent(float forward, float sideways) {
		this.forward = forward;
		this.sideways = sideways;
	}
}
