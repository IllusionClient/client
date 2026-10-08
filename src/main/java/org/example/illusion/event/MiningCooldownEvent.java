package org.example.illusion.event;

public final class MiningCooldownEvent implements Event {
	public int cooldown;

	public MiningCooldownEvent(int cooldown) {
		this.cooldown = cooldown;
	}
}
