package org.codeberg.illusionclient.event;

import net.minecraft.client.render.Window;

public final class Render2DEvent implements Event {
	private static final Render2DEvent INSTANCE = new Render2DEvent();
	private float partialTicks;
	private Window window;

	private Render2DEvent() {
	}

	public static Render2DEvent of(float partialTicks, Window window) {
		INSTANCE.partialTicks = partialTicks;
		INSTANCE.window = window;
		return INSTANCE;
	}

	public float partialTicks() {
		return partialTicks;
	}

	public Window window() {
		return window;
	}
}
