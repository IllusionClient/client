package org.codeberg.illusionclient.event;

public final class AttackEvent implements Event {
	private static final AttackEvent INSTANCE = new AttackEvent();
	public boolean resetCooldown;

	private AttackEvent() {
	}

	public static AttackEvent of() {
		INSTANCE.resetCooldown = false;
		return INSTANCE;
	}
}
