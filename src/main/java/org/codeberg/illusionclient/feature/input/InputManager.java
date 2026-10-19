package org.codeberg.illusionclient.feature.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.InputEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public final class InputManager {
	public final InputState player = new InputState();
	public final InputState client = new InputState();
	private final Minecraft minecraft = Minecraft.getInstance();
	private final GameOptions options = minecraft.options;

	public boolean held(KeyBinding key) {
		if (minecraft.screen != null) return false;
		int code = key.getKeyCode();
		return code < 0 ? Mouse.isButtonDown(code + 100) : code > 0 && Keyboard.isKeyDown(code);
	}

	public void update() {
		player.forward = held(options.forwardKey);
		player.back = held(options.backKey);
		player.left = held(options.leftKey);
		player.right = held(options.rightKey);
		player.jump = held(options.jumpKey);
		player.sneak = held(options.sneakKey);
		client.copy(player);
		IllusionClient.BUS.publish(InputEvent.INSTANCE);
	}

	public boolean resolve(KeyBinding key, boolean pressed) {
		if (key == options.forwardKey) return resolve(player.forward, client.forward, pressed);
		if (key == options.backKey) return resolve(player.back, client.back, pressed);
		if (key == options.leftKey) return resolve(player.left, client.left, pressed);
		if (key == options.rightKey) return resolve(player.right, client.right, pressed);
		if (key == options.jumpKey) return resolve(player.jump, client.jump, pressed);
		if (key == options.sneakKey) return resolve(player.sneak, client.sneak, pressed);
		return pressed;
	}

	private static boolean resolve(boolean player, boolean client, boolean pressed) {
		return player == client ? pressed : client;
	}
}
