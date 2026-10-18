package org.codeberg.illusionclient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class Main {
	//? if fabric {
	private static final String LOADER = "Ornithe";
	private static final String LOADER_URL = "https://ornithemc.net/";
	//?} else {
	/*private static final String LOADER = "Forge";
	private static final String LOADER_URL = "https://files.minecraftforge.net/net/minecraftforge/forge/index_1.8.9.html";
	*///?}
	private static final String DISCORD_URL = "https://discord.com/invite/rzgm3C8YWu";
	private static final String MESSAGE =
		"This is a " + LOADER + " mod, not an application.\n\n" +
			"If you don't know what " + LOADER + " is, or don't know how to install it,\n" +
			"please visit: " + LOADER_URL;

	private static final String TITLE = "Improper Installation Method";
	private static final String[] OPTIONS = {LOADER, "Discord", "Close"};

	public static void main(String[] args) {
		System.err.println(MESSAGE);

		if (!GraphicsEnvironment.isHeadless()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}

			int choice = JOptionPane.showOptionDialog(null, MESSAGE, TITLE, JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, OPTIONS, OPTIONS[0]);
			if (choice == 0) browse(LOADER_URL);
			if (choice == 1) browse(DISCORD_URL);
		}

		System.exit(0);
	}

	private static void browse(String url) {
		try {
			Desktop.getDesktop().browse(URI.create(url));
		} catch (Exception e) {
			System.err.println("Could not open " + url);
		}
	}
}
