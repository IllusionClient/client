package org.codeberg.illusionclient.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.input.Mouse;

public final class MouseUtils {
	private static int button;
	private static int pendingEvents;
	private static boolean synthetic;

	public static void click(int button) {
		if (button != MouseUtils.button) pendingEvents = 0;
		MouseUtils.button = button;
		pendingEvents += 2;
	}

	public static boolean next(boolean hasEvent) {
		if (hasEvent) {
			synthetic = false;
			return true;
		}
		if ((pendingEvents & 1) == 0 && !(Minecraft.getInstance().screen == null && Mouse.isButtonDown(button))) {
			pendingEvents = 0;
		}
		if (pendingEvents == 0) return synthetic = false;
		pendingEvents--;
		return synthetic = true;
	}

	public static boolean synthetic() {
		return synthetic;
	}

	public static int eventButton() {
		return button;
	}

	public static boolean eventState() {
		return (pendingEvents & 1) == 0;
	}
}
