package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.FrustumData;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.text.Formatting;
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
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.util.ColorUtils;
import org.codeberg.illusionclient.util.PlayerUtils;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

@Description("Draws boxes and bars around players.")
@ModuleInfo(name = "ESP", category = Category.VISUAL)
public class Esp extends Module {
	private static final int OUTLINE = 0x96000000;
	private final List<PlayerEntity> players = new ObjectArrayList<>();
	private float[] bounds = new float[64];
	private float pulseFactor;

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
	@Check("Armor Bar")
	public boolean armorBar = true;

	@Description("The color of the armor bar.")
	@Requires("armorBar")
	@ColorPicker("Armor Color")
	public Color armorColor = new Color(0, 255, 255);

	@Description("Draws a health bar beside the box.")
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

	private static void outline(float left, float top, float right, float bottom, float thickness, int color) {
		thickness = Math.min(thickness, Math.min(right - left, bottom - top) / 2.0F);

		Draw.fill(left, top, left + thickness, bottom, color);
		Draw.fill(right - thickness, top, right, bottom, color);
		Draw.fill(left + thickness, top, right - thickness, top + thickness, color);
		Draw.fill(left + thickness, bottom - thickness, right - thickness, bottom, color);
	}

	@Override
	public void onDisable() {
		players.clear();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		players.clear();
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		players.clear();

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.world == null || minecraft.player == null) return;

		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		FrustumData data = Frustum.getInstance();
		FrustumCuller frustum = new FrustumCuller(data);
		frustum.prepare(dispatcher.cameraX, dispatcher.cameraY, dispatcher.cameraZ);

		float[] clip = data.clipMatrix;
		float halfWidth = minecraft.width / 2.0F;
		float halfHeight = minecraft.height / 2.0F;

		float tickDelta = event.partialTicks();
		for (PlayerEntity player : minecraft.world.players) {
			if (!valid(minecraft, player) || !frustum.isVisible(player.getShape())) continue;

			double x = player.prevX + (player.x - player.prevX) * tickDelta - dispatcher.cameraX;
			double y = player.prevY + (player.y - player.prevY) * tickDelta - dispatcher.cameraY;
			double z = player.prevZ + (player.z - player.prevZ) * tickDelta - dispatcher.cameraZ;
			double radius = player.width / 2.0D + 0.1D;
			double bottom = y - 0.1D;
			double top = y + player.height + (player.isSneaking() ? -0.2D : 0.1D) + 0.1D;

			float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -1.0F, maxY = -1.0F;
			for (int corner = 0; corner < 8; corner++) {
				float cornerX = (float) (x + ((corner & 1) == 0 ? -radius : radius));
				float cornerY = (float) ((corner & 2) == 0 ? bottom : top);
				float cornerZ = (float) (z + ((corner & 4) == 0 ? -radius : radius));

				float w = clip[3] * cornerX + clip[7] * cornerY + clip[11] * cornerZ + clip[15];
				if (w == 0.0F) continue;

				float depth = (clip[2] * cornerX + clip[6] * cornerY + clip[10] * cornerZ + clip[14]) / w;
				if (depth < -1.0F || depth >= 1.0F) continue;

				float screenX = ((clip[0] * cornerX + clip[4] * cornerY + clip[8] * cornerZ + clip[12]) / w + 1.0F) * halfWidth;
				float screenY = (1.0F - (clip[1] * cornerX + clip[5] * cornerY + clip[9] * cornerZ + clip[13]) / w) * halfHeight;
				minX = Math.min(minX, screenX);
				minY = Math.min(minY, screenY);
				maxX = Math.max(maxX, screenX);
				maxY = Math.max(maxY, screenY);
			}
			if (maxX < minX) continue;

			int index = players.size() * 4;
			if (index == bounds.length) bounds = Arrays.copyOf(bounds, index * 2);
			bounds[index] = minX;
			bounds[index + 1] = minY;
			bounds[index + 2] = maxX;
			bounds[index + 3] = maxY;
			players.add(player);
		}
	}

	@Listen
	public void onRender2D(Render2DEvent.Pre event) {
		if (players.isEmpty()) return;

		float progress = System.currentTimeMillis() % 2000L / 1000.0F;
		pulseFactor = pulsing ? 1.0F - 0.4F * (progress > 1.0F ? 2.0F - progress : progress) : 1.0F;
		int boxRgb = boxColor.getRGB();

		GlStateManager.depthMask(false);
		GlStateManager.pushMatrix();
		double pixel = 1.0D / event.window().getScale();
		GlStateManager.scaled(pixel, pixel, 1.0D);

		for (int i = 0; i < players.size(); i++) {
			PlayerEntity player = players.get(i);
			int left = Math.round(bounds[i * 4]);
			int top = Math.round(bounds[i * 4 + 1]);
			int right = Math.round(bounds[i * 4 + 2]);
			int bottom = Math.round(bounds[i * 4 + 3]);

			if (armorBar) renderArmorBar(player, left, right, bottom);
			if (healthBar) renderHealthBar(player, left, top, bottom);

			if (box) {
				outline(left, top, right, bottom, 3.0F, OUTLINE);
				outline(left + 1, top + 1, right - 1, bottom - 1, 1.0F, teamColor ? teamColor(player, boxRgb) : boxRgb);
			}
		}
		Draw.flush();

		GlStateManager.popMatrix();
		GlStateManager.depthMask(true);
		GlStateManager.enableBlend();
	}

	private void renderArmorBar(PlayerEntity player, int left, int right, int bottom) {
		float armor = Math.min(1.0F, player.getArmorProtection() / 20.0F);

		Draw.fill(left, bottom + 1, right, bottom + 5, OUTLINE);

		int fillRight = left + Math.round((right - left) * armor) - 1;
		if (fillRight > left + 1) Draw.fill(left + 1, bottom + 2, fillRight, bottom + 4, armorColor.getRGB());
	}

	private void renderHealthBar(PlayerEntity player, int boxLeft, int top, int bottom) {
		int left = boxLeft - 4;
		int right = boxLeft - 2;
		int barBottom = bottom - 1;
		int height = bottom - top;
		float health = Math.clamp(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);

		Draw.fill(left - 1, top, right + 1, bottom, OUTLINE);

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
