package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Formatting;
import net.minecraft.util.math.Box;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.render.EspRenderer;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.util.ColorUtils;
import org.codeberg.illusionclient.util.PlayerUtils;

import java.awt.*;

@Description("Draws boxes and bars around players.")
@ModuleInfo(name = "ESP", category = Category.VISUAL)
public class Esp extends Module {
	private final EspRenderer<PlayerEntity> renderer = new EspRenderer<>();
	private float pulseFactor;

	@Description("2D draws flat boxes and bars on screen, 3D draws boxes in the world.")
	@Combo(value = "Mode", options = {"2D", "3D"})
	public String mode = "2D";

	@Description("Draws a box around players.")
	@Check("Box")
	public boolean box = true;

	@Description("The color of the box.")
	@Requires("box")
	@ColorPicker("Box Color")
	public Color boxColor = new Color(0x74C3FF);

	@Description("Colors the box with the player's nametag color.")
	@Requires("box")
	@Check("Team Color")
	public boolean teamColor = false;

	@Description("Draws an armor bar below the box.")
	@Requires(value = "mode", is = "2D")
	@Check("Armor Bar")
	public boolean armorBar = true;

	@Description("The color of the armor bar.")
	@Requires("armorBar")
	@ColorPicker("Armor Color")
	public Color armorColor = new Color(0, 255, 255);

	@Description("Draws a health bar beside the box.")
	@Requires(value = "mode", is = "2D")
	@Check("HP Bar")
	public boolean healthBar = true;

	@Description("Solid color, green to red by health, or a gradient.")
	@Requires("healthBar")
	@Combo(value = "HP Color Mode", options = {"Solid", "Health", "Gradient"})
	public String healthMode = "Solid";

	@Description("Health bar color, or the gradient's top.")
	@Requires(value = "healthMode", is = {"Solid", "Gradient"})
	@ColorPicker("HP Color")
	public Color healthColor = new Color(0, 255, 89);

	@Description("The bottom color of the gradient.")
	@Requires(value = "healthMode", is = "Gradient")
	@ColorPicker("HP End")
	public Color healthEndColor = new Color(0xF44336);

	@Description("Absorption color on the health bar.")
	@Requires("healthBar")
	@ColorPicker("Absorption")
	public Color absorptionColor = new Color(0xDD00FF);

	@Description("Makes the health bar slowly pulse.")
	@Requires("healthBar")
	@Check("Pulsing HP")
	public boolean pulsing = true;

	@Description("Also draws invisible players.")
	@Check("Invisibles")
	public boolean invisibles = false;

	@Description("Also draws you in third person.")
	@Check("Self")
	public boolean self = true;

	@Override
	public void onDisable() {
		renderer.clear();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		renderer.clear();
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		renderer.clear();

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.world == null || minecraft.player == null) return;

		renderer.begin();
		boolean flat = mode.equals("2D");
		int boxRgb = boxColor.getRGB();
		float tickDelta = event.partialTicks();
		for (PlayerEntity player : minecraft.world.players) {
			if (!valid(minecraft, player)) continue;

			double x = player.prevX + (player.x - player.prevX) * tickDelta;
			double y = player.prevY + (player.y - player.prevY) * tickDelta;
			double z = player.prevZ + (player.z - player.prevZ) * tickDelta;
			double radius = player.width / 2.0D + 0.1D;
			double top = y + player.height + (player.isSneaking() ? -0.2D : 0.1D) + 0.1D;
			Box box = new Box(x - radius, y - 0.1D, z - radius, x + radius, top, z + radius);

			if (flat) renderer.add(player, box);
			else if (this.box) renderer.box3D(box, teamColor ? teamColor(player, boxRgb) : boxRgb);
		}
		renderer.end3D();
	}

	@Listen
	public void onRender2D(Render2DEvent.Pre event) {
		float progress = System.currentTimeMillis() % 2000L / 1000.0F;
		pulseFactor = pulsing ? 1.0F - 0.4F * (progress > 1.0F ? 2.0F - progress : progress) : 1.0F;
		int boxRgb = boxColor.getRGB();

		renderer.render2D(event, (player, left, top, right, bottom) -> {
			if (armorBar) renderArmorBar(player, left, right, bottom);
			if (healthBar) renderHealthBar(player, left, top, bottom);
			if (box) EspRenderer.box(left, top, right, bottom, teamColor ? teamColor(player, boxRgb) : boxRgb);
		});
	}

	private void renderArmorBar(PlayerEntity player, int left, int right, int bottom) {
		float armor = Math.min(1.0F, player.getArmorProtection() / 20.0F);

		Draw.fill(left, bottom + 1, right, bottom + 5, EspRenderer.OUTLINE);

		int fillRight = left + Math.round((right - left) * armor) - 1;
		if (fillRight > left + 1) Draw.fill(left + 1, bottom + 2, fillRight, bottom + 4, armorColor.getRGB());
	}

	private void renderHealthBar(PlayerEntity player, int boxLeft, int top, int bottom) {
		int left = boxLeft - 4;
		int right = boxLeft - 2;
		int barBottom = bottom - 1;
		int height = bottom - top;
		float health = Math.clamp(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);

		Draw.fill(left - 1, top, right + 1, bottom, EspRenderer.OUTLINE);

		int healthTop = bottom + 1 - Math.round(height * health);
		if (healthTop < barBottom) {
			if (healthMode.equals("Gradient")) {
				Draw.fill(left, healthTop, right, barBottom, pulse(healthColor.getRGB()), pulse(healthEndColor.getRGB()));
			} else {
				int color = healthMode.equals("Health") ? Color.HSBtoRGB(health / 3.0F, 0.9F, 0.9F) : healthColor.getRGB();
				Draw.fill(left, healthTop, right, barBottom, pulse(color));
			}
		}

		float absorption = Math.min(1.0F, player.getAbsorption() / 20.0F);
		int absorptionTop = bottom + 1 - Math.round(height * absorption);
		if (absorptionTop < barBottom)
			Draw.fill(left, absorptionTop, right, barBottom, pulse(absorptionColor.getRGB()));
	}

	private boolean valid(Minecraft minecraft, PlayerEntity player) {
		if (!player.isAlive()) return false;
		if (player.isInvisible() && !invisibles) return false;

		boolean firstPerson = minecraft.options.perspective == 0;
		if (player == minecraft.player) return self && !firstPerson;
		return !firstPerson || player.distanceTo(minecraft.player) >= 1.0F;
	}

	private static int teamColor(PlayerEntity player, int fallback) {
		Formatting color = PlayerUtils.nametagColor(player);
		if (color == null) return fallback;

		int id = color.getId();
		int light = (id >> 3 & 1) * 85;
		int red = (id >> 2 & 1) * 170 + light + (id == 6 ? 85 : 0);
		int green = (id >> 1 & 1) * 170 + light;
		int blue = (id & 1) * 170 + light;
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private int pulse(int color) {
		return ColorUtils.darken(color, pulseFactor);
	}
}
