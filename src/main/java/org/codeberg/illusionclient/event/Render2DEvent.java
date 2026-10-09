package org.codeberg.illusionclient.event;

import net.minecraft.client.render.Window;

public abstract class Render2DEvent implements Event {
	private float partialTicks;
	private Window window;

	private Render2DEvent() {
	}

	public float partialTicks() {
		return partialTicks;
	}

	public Window window() {
		return window;
	}

	private static <T extends Render2DEvent> T set(T event, float partialTicks, Window window) {
		Render2DEvent base = event;
		base.partialTicks = partialTicks;
		base.window = window;
		return event;
	}

	public static final class Pre extends Render2DEvent {
		private static final Pre INSTANCE = new Pre();

		public static Pre of(float partialTicks, Window window) {
			return set(INSTANCE, partialTicks, window);
		}
	}

	public static final class Post extends Render2DEvent {
		private static final Post INSTANCE = new Post();

		public static Post of(float partialTicks, Window window) {
			return set(INSTANCE, partialTicks, window);
		}
	}
}
