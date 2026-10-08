package org.example.illusion.event;

public final class JumpYawEvent implements Event {
	public float yaw;

	public JumpYawEvent(float yaw) {
		this.yaw = yaw;
	}
}
