package org.example.illusion.event;

public final class MiningCooldownEvent implements Event {
	private static final MiningCooldownEvent INSTANCE = new MiningCooldownEvent();
	public int cooldown;

	private MiningCooldownEvent() {
	}

	public static MiningCooldownEvent of(int cooldown) {
		INSTANCE.cooldown = cooldown;
		return INSTANCE;
	}
}
