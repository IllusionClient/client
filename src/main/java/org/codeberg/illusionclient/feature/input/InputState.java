package org.codeberg.illusionclient.feature.input;

public final class InputState {
	public boolean forward;
	public boolean back;
	public boolean left;
	public boolean right;
	public boolean jump;
	public boolean sneak;

	public float forwardAxis() {
		return axis(forward, back);
	}

	public float sidewaysAxis() {
		return axis(left, right);
	}

	void copy(InputState other) {
		forward = other.forward;
		back = other.back;
		left = other.left;
		right = other.right;
		jump = other.jump;
		sneak = other.sneak;
	}

	private static float axis(boolean positive, boolean negative) {
		return (positive ? 1.0F : 0.0F) - (negative ? 1.0F : 0.0F);
	}
}
