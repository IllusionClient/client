package org.example.illusion.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.FrustumCuller;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.entity.living.player.PlayerEntity;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.ColorPicker;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.event.Render2DEvent;
import org.example.illusion.event.Render3DEvent;
import org.example.illusion.event.WorldLoadEvent;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;
import org.example.illusion.feature.ui.Draw;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

@ModuleInfo(name = "ESP", category = Category.VISUAL)
public class Esp extends Module {
    private static final int OUTLINE = 0x96000000;

    @Check("Box")
    public boolean box = true;

    @ColorPicker("Box Color")
    public Color boxColor = new Color(0x74C3FF);

    @Check("Armor Bar")
    public boolean armorBar = true;

    @ColorPicker("Armor Color")
    public Color armorColor = new Color(0, 255, 255);

    @Check("HP Bar")
    public boolean healthBar = true;

    @Combo(value = "HP Color Mode", options = {"Solid", "Health", "Gradient"})
    public String healthMode = "Solid";

    @ColorPicker("HP Color")
    public Color healthColor = new Color(0, 255, 89);

    @ColorPicker("HP End")
    public Color healthEndColor = new Color(0xF44336);

    @ColorPicker("Absorption")
    public Color absorptionColor = new Color(0xDD00FF);

    @Check("Pulsing HP")
    public boolean pulsing = true;

    @Check("Invisibles")
    public boolean invisibles = false;

    @Check("Self")
    public boolean self = true;

    private final Map<PlayerEntity, float[]> bounds = new HashMap<>();
    private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
    private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
    private final IntBuffer viewport = BufferUtils.createIntBuffer(16);
    private final FloatBuffer projected = BufferUtils.createFloatBuffer(4);

    @Override
    public void onDisable() {
        bounds.clear();
    }

    @Listen
    public void onWorldLoad(WorldLoadEvent event) {
        bounds.clear();
    }

    @Listen
    public void onRender3D(Render3DEvent event) {
        bounds.clear();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.world == null || minecraft.player == null) return;

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        FrustumCuller frustum = new FrustumCuller();
        frustum.prepare(dispatcher.cameraX, dispatcher.cameraY, dispatcher.cameraZ);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        float tickDelta = event.partialTicks;
        for (PlayerEntity player : minecraft.world.players) {
            if (!valid(minecraft, player) || !frustum.isVisible(player.getShape())) continue;

            double x = player.prevX + (player.x - player.prevX) * tickDelta - dispatcher.cameraX;
            double y = player.prevY + (player.y - player.prevY) * tickDelta - dispatcher.cameraY;
            double z = player.prevZ + (player.z - player.prevZ) * tickDelta - dispatcher.cameraZ;
            double halfWidth = player.width / 2.0D + 0.1D;
            double bottom = y - 0.1D;
            double top = y + player.height + (player.isSneaking() ? -0.2D : 0.1D) + 0.1D;

            float[] rect = {Float.MAX_VALUE, Float.MAX_VALUE, -1.0F, -1.0F};
            for (int corner = 0; corner < 8; corner++) {
                float cornerX = (float) (x + ((corner & 1) == 0 ? -halfWidth : halfWidth));
                float cornerY = (float) ((corner & 2) == 0 ? bottom : top);
                float cornerZ = (float) (z + ((corner & 4) == 0 ? -halfWidth : halfWidth));

                if (!GLU.gluProject(cornerX, cornerY, cornerZ, modelView, projection, viewport, projected)) continue;
                if (projected.get(2) < 0.0F || projected.get(2) >= 1.0F) continue;

                float screenX = projected.get(0);
                float screenY = minecraft.height - projected.get(1);
                rect[0] = Math.min(rect[0], screenX);
                rect[1] = Math.min(rect[1], screenY);
                rect[2] = Math.max(rect[2], screenX);
                rect[3] = Math.max(rect[3], screenY);
            }

            if (rect[2] >= rect[0]) bounds.put(player, rect);
        }
    }

    @Listen
    public void onRender2D(Render2DEvent event) {
        if (bounds.isEmpty()) return;

        GlStateManager.depthMask(false);

        float scale = event.window.getScale();
        for (Map.Entry<PlayerEntity, float[]> entry : bounds.entrySet()) {
            PlayerEntity player = entry.getKey();
            float left = entry.getValue()[0] / scale;
            float top = entry.getValue()[1] / scale;
            float right = entry.getValue()[2] / scale;
            float bottom = entry.getValue()[3] / scale;

            if (armorBar) renderArmorBar(player, left, right, bottom);
            if (healthBar) renderHealthBar(player, left, top, bottom);

            if (box) {
                outline(left, top, right, bottom, 1.5F, OUTLINE);
                outline(left + 0.5F, top + 0.5F, right - 0.5F, bottom - 0.5F, 0.5F, boxColor.getRGB());
            }
        }
        Draw.flush();

        GlStateManager.depthMask(true);
        GlStateManager.enableBlend();
    }

    private void renderArmorBar(PlayerEntity player, float left, float right, float bottom) {
        float armor = Math.min(1.0F, player.getArmorProtection() / 20.0F);

        Draw.fill(left, bottom + 0.5F, right, bottom + 2.5F, OUTLINE);
        if (armor > 0.0F) {
            Draw.fill(left + 0.5F, bottom + 1.0F, left + (right - left) * armor - 0.5F, bottom + 2.0F, armorColor.getRGB());
        }
    }

    private void renderHealthBar(PlayerEntity player, float boxLeft, float top, float bottom) {
        float left = boxLeft - 2.0F;
        float right = boxLeft - 1.0F;
        float barBottom = bottom - 0.5F;
        float height = bottom - top;
        float health = Math.clamp(player.getHealth() / player.getMaxHealth(), 0.0F, 1.0F);

        Draw.fill(left - 0.5F, top, right + 0.5F, bottom, OUTLINE);

        float healthTop = bottom + 0.5F - height * health;
        if (healthTop < barBottom) {
            if (healthMode.equals("Gradient")) {
                Draw.fill(left, healthTop, right, barBottom, pulse(healthColor.getRGB()), pulse(healthEndColor.getRGB()));
            } else {
                int color = healthMode.equals("Health") ? Color.HSBtoRGB(health / 3.0F, 0.9F, 0.9F) : healthColor.getRGB();
                Draw.fill(left, healthTop, right, barBottom, pulse(color));
            }
        }

        float absorption = Math.min(1.0F, player.getAbsorption() / 20.0F);
        float absorptionTop = bottom + 0.5F - height * absorption;
        if (absorptionTop < barBottom) Draw.fill(left, absorptionTop, right, barBottom, pulse(absorptionColor.getRGB()));
    }

    private boolean valid(Minecraft minecraft, PlayerEntity player) {
        if (!player.isAlive()) return false;
        if (player.isInvisible() && !invisibles) return false;

        boolean firstPerson = minecraft.options.perspective == 0;
        if (player == minecraft.player) return self && !firstPerson;
        return !firstPerson || player.distanceTo(minecraft.player) >= 1.0F;
    }

    private int pulse(int color) {
        if (!pulsing) return color;

        float progress = System.currentTimeMillis() % 2000L / 1000.0F;
        float factor = 1.0F - 0.4F * (progress > 1.0F ? 2.0F - progress : progress);
        return color & 0xFF000000
                | (int) ((color >> 16 & 255) * factor) << 16
                | (int) ((color >> 8 & 255) * factor) << 8
                | (int) ((color & 255) * factor);
    }

    private static void outline(float left, float top, float right, float bottom, float thickness, int color) {
        Draw.fill(left, top, left + thickness, bottom, color);
        Draw.fill(right - thickness, top, right, bottom, color);
        Draw.fill(left + thickness, top, right - thickness, top + thickness, color);
        Draw.fill(left + thickness, bottom - thickness, right - thickness, bottom, color);
    }
}
