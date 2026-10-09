package org.codeberg.illusionclient.event;

public final class KeyPressEvent implements Event {
	private static final KeyPressEvent INSTANCE = new KeyPressEvent();
	private int key;
	private boolean down;

	private KeyPressEvent() {
	}

	public static KeyPressEvent of(int key, boolean down) {
		INSTANCE.key = key;
		INSTANCE.down = down;
		return INSTANCE;
	}

	public int key() {
		return key;
	}

	public boolean down() {
		return down;
	}
}
