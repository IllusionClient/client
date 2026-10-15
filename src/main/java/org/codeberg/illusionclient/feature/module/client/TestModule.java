package org.codeberg.illusionclient.feature.module.client;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.render.platform.GlStateManager;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.codeberg.illusionclient.event.UseDelayEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.RangeSlider;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.lwjgl.input.Keyboard;

import java.awt.*;

@ModuleInfo(name = "Test", category = Category.CLIENT, bind = Keyboard.KEY_BACKSLASH)
public class TestModule extends Module {
	private static final int LINE_HEIGHT = 10;

	@Check("Check")
	public boolean check = true;

	@Requires("check")
	@Slider(value = "Slider", min = 0.0F, max = 10.0F)
	public float slider = 5.0F;

	@RangeSlider(value = "Range Slider", min = 0.0F, max = 100.0F)
	public float[] rangeSlider = {25.0F, 75.0F};

	@Combo(value = "Combo", options = {"First", "Second", "Third"})
	public String combo = "First";

	@Requires(value = "combo", is = {"Second", "Third"})
	@ColorPicker("Color Picker")
	public Color colorPicker = new Color(46, 204, 113);

	@Requires(value = "combo", is = "Third")
	@Check("Cap Use Delay")
	public boolean capUseDelay = false;

	private int ticks;

	@Override
	public String details() {
		return combo + " " + number(slider);
	}

	@Override
	public void setBind(int bind) {
		super.setBind(bind);
		IllusionClient.LOGGER.info("Test bound to {}", Keyboard.getKeyName(bind));
	}

	@Override
	public void onEnable() {
		ticks = 0;
		IllusionClient.LOGGER.info("Test enabled");
	}

	@Override
	public void onDisable() {
		IllusionClient.LOGGER.info("Test disabled after {} ticks", ticks);
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		ticks++;
	}

	@Listen
	public void onUseDelay(UseDelayEvent event) {
		if (capUseDelay) event.delay = Math.min(event.delay, Math.round(slider));
	}

	@Listen
	public void onRender2D(Render2DEvent.Post event) {
		String[] lines = {
			name + " [" + category + "] " + Keyboard.getKeyName(getBind()),
			"Enabled for " + ticks + " ticks",
			"Check: " + check,
			"Slider: " + number(slider),
			"Range Slider: " + number(rangeSlider[0]) + " - " + number(rangeSlider[1]),
			"Combo: " + combo,
			"Cap Use Delay: " + capUseDelay,
			"Color Picker:"
		};

		int y = 2;
		for (String line : lines) {
			Draw.text(line, 2.0F, y, 0xFFFFFFFF);
			y += LINE_HEIGHT;
		}
		Draw.rect(2, y, 40, LINE_HEIGHT, colorPicker.getRGB());
		Draw.flush();

		GlStateManager.enableBlend();
	}
}
