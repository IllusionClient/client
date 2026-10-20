package org.codeberg.illusionclient.feature.ui;

import net.minecraft.resource.Identifier;
import org.codeberg.illusionclient.feature.module.api.Category;

public enum Icon {
	HOME("home", 8, 8),
	COMBAT("combat", 8, 8),
	MOVEMENT("movement", 8, 8),
	PLAYER("player", 8, 8),
	VISUAL("visual", 8, 8),
	CLIENT("client", 8, 8),
	SEARCH("search", 8, 8),
	CHEVRON("chevron", 4, 7),
	LOGO("logo", 9, 8);

	public final int width, height;
	private final Identifier texture, smooth;

	Icon(String name, int width, int height) {
		this.texture = new Identifier("illusion", "textures/gui/icons/" + name + ".png");
		this.smooth = new Identifier("illusion", "textures/gui/icons/smooth/" + name + ".png");
		this.width = width;
		this.height = height;
	}

	public static Icon of(Category category) {
		return switch (category) {
			case CLIENT -> CLIENT;
			case COMBAT -> COMBAT;
			case MOVEMENT -> MOVEMENT;
			case PLAYER -> PLAYER;
			case VISUAL -> VISUAL;
		};
	}

	public void draw(int x, int y, int color) {
		Draw.texture(Draw.smooth() ? smooth : texture, x, y, width, height, color);
	}
}
