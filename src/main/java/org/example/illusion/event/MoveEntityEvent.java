package org.example.illusion.event;

public final class MoveEntityEvent extends CancellableEvent {
	public double x, y, z;

	public MoveEntityEvent(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}
