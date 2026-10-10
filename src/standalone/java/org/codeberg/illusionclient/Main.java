package org.codeberg.illusionclient;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

public class Main {
	private static final String ORNITHE_URL = "https://ornithemc.net/";
	private static final String CODEBERG_URL = "https://codeberg.org/IllusionClient/Illusion/";
	private static final String MESSAGE =
		"This is an Ornithe mod, not an application.\n\n" +
			"If you don't know what Ornithe is, or don't know how to install it,\n" +
			"please visit: " + ORNITHE_URL;

	private static final String TITLE = "Improper Installation Method";
	private static final String[] OPTIONS = {"Ornithe", "Codeberg", "Close"};

	public static void main(String[] args) {
		System.err.println(MESSAGE);

		if (!GraphicsEnvironment.isHeadless()) {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception ignored) {
			}

			int choice = JOptionPane.showOptionDialog(null, MESSAGE, TITLE, JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, OPTIONS, OPTIONS[0]);
			if (choice == 0) browse(ORNITHE_URL);
			if (choice == 1) browse(CODEBERG_URL);
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
