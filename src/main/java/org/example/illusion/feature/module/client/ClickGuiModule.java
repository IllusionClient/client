package org.example.illusion.feature.module.client;

import net.minecraft.client.Minecraft;
import org.example.illusion.feature.config.api.ColorPicker;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;
import org.example.illusion.feature.ui.ClickGui;
import org.lwjgl.input.Keyboard;

import java.awt.*;

@ModuleInfo(name = "ClickGui", category = Category.CLIENT, bind = Keyboard.KEY_RSHIFT)
public class ClickGuiModule extends Module {
	@ColorPicker("Color")
	public Color color = new Color(46, 204, 113);

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
