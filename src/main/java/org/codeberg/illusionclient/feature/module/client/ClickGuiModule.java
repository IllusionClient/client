package org.codeberg.illusionclient.feature.module.client;

import net.minecraft.client.Minecraft;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.ui.ClickGui;
import org.lwjgl.input.Keyboard;

import java.awt.*;

@Description("Opens this menu.")
@ModuleInfo(name = "ClickGui", category = Category.CLIENT, bind = Keyboard.KEY_RSHIFT)
public class ClickGuiModule extends Module {
	@Description("The accent color of this menu.")
	@ColorPicker("Color")
	public Color color = new Color(41, 227, 143);

	private ClickGui gui;

	@Override
	public void setBind(int bind) {
		super.setBind(bind == Keyboard.KEY_NONE ? Keyboard.KEY_RSHIFT : bind);
	}

	@Override
	public void onEnable() {
		if (gui == null) gui = new ClickGui();

		Minecraft minecraft = Minecraft.getInstance();
		minecraft.openScreen(minecraft.screen == gui ? null : gui);
		setEnabled(false);
	}
}
