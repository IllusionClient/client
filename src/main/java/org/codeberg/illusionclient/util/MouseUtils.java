package org.codeberg.illusionclient.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public final class MouseUtils {
	private static int button;
	private static int pendingEvents;
	private static boolean lifted;
	private static boolean synthetic;

	public static void toggle(int button) {
		if (button != MouseUtils.button) {
			pendingEvents = 0;
			lifted = false;
		}
		MouseUtils.button = button;
		pendingEvents++;
	}

	public static void resync() {
		pendingEvents = lifted && Mouse.isButtonDown(button) ? 1 : 0;
		if (pendingEvents == 0) lifted = false;
	}

	public static boolean next(boolean hasEvent) {
		if (hasEvent) {
			synthetic = false;
			return true;
		}
		if (lifted && !(Minecraft.getInstance().screen == null && Mouse.isButtonDown(button))) {
			pendingEvents = 0;
			lifted = false;
		}
		if (pendingEvents == 0) return synthetic = false;
		pendingEvents--;
		lifted = !lifted;
		return synthetic = true;
	}

	public static boolean synthetic() {
		return synthetic;
	}

	public static int eventButton() {
		return button;
	}

	public static boolean eventState() {
		return !lifted;
	}
}
