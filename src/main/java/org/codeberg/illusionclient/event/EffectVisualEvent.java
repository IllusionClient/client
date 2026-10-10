package org.codeberg.illusionclient.event;

import net.minecraft.entity.living.effect.StatusEffect;

public final class EffectVisualEvent implements Event {
	private static final EffectVisualEvent INSTANCE = new EffectVisualEvent();
	public StatusEffect effect;
	public boolean visible;

	private EffectVisualEvent() {
	}

	public static EffectVisualEvent of(StatusEffect effect, boolean visible) {
		INSTANCE.effect = effect;
		INSTANCE.visible = visible;
		return INSTANCE;
	}
}
