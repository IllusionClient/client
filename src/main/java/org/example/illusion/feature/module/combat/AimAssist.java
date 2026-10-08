package org.example.illusion.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.example.illusion.feature.config.api.Check;
import org.example.illusion.feature.config.api.Combo;
import org.example.illusion.feature.config.api.RangeSlider;
import org.example.illusion.feature.config.api.Slider;
import org.example.illusion.event.Render3DEvent;
import org.example.illusion.event.WorldLoadEvent;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;
import org.example.illusion.util.CombatUtils;
import org.lwjgl.input.Mouse;

import java.util.Random;

@ModuleInfo(name = "Aim Assist", category = Category.COMBAT)
@Description("Moves your crosshair onto other players for you.")
public class AimAssist extends Module {
    private static final float PREDICT_TICKS = 3.0F;
    private static final long MOUSE_MOVE_WINDOW = 150L;
    private static final double EDGE_INSET = 0.05D;

    @Combo(value = "Mode", options = {"Regular", "Linear", "Lock-on"})
    public String mode = "Regular";

    @Slider(value = "Horizontal Speed", min = 0.0F, max = 100.0F)
    public float horizontalSpeed = 30.0F;

    @Slider(value = "Vertical Speed", min = 0.0F, max = 100.0F)
    public float verticalSpeed = 15.0F;

    @Slider(value = "Horizontal Multipoint", min = 0.0F, max = 100.0F)
    public float horizontalMultipoint = 0.0F;

    @Slider(value = "Vertical Multipoint", min = 0.0F, max = 100.0F)
    public float verticalMultipoint = 0.0F;

    @Slider(value = "Predict", min = 0.0F, max = 100.0F)
    public float predict = 0.0F;

    @Slider(value = "Randomization", min = 0.0F, max = 100.0F)
    public float randomization = 50.0F;

    @Combo(value = "Target Mode", options = {"Single", "Switch"})
    public String targetMode = "Single";

    @Check("Increased FOV While Locked")
    public boolean increasedFov = false;

    @Combo(value = "Sort By", options = {"Distance", "Health", "Hurt time", "Aim angle"})
    public String sortBy = "Distance";

    @RangeSlider(value = "Range", min = 0.0F, max = 8.0F)
    public float[] range = {0.0F, 4.5F};

    @RangeSlider(value = "FOV", min = 0.0F, max = 360.0F)
    public float[] fov = {0.0F, 90.0F};

    @Check("Sprinting")
    public boolean sprinting = false;

    @Check("Mouse Pressed")
    public boolean mousePressed = true;

    @Check("Mouse Moved")
    public boolean mouseMoved = false;

    @Check("Disable While Breaking Blocks")
    public boolean breakingBlocks = true;

    @Check("Holding Weapon")
    public boolean holdingWeapon = true;

    @Check("Ignore Invisibles")
    public boolean ignoreInvisibles = true;

    @Check("Not Behind Blocks")
    public boolean notBehindBlocks = true;

    private final Minecraft minecraft = Minecraft.getInstance();
    private final Random random = new Random();
    private final float[] noise = new float[3];
    private final float[] noiseGoal = new float[3];

    private PlayerEntity target;
    private long lastFrame;
    private long lastMouseMove;
    private long nextNoise;
    private float yawRemainder;
    private float pitchRemainder;

    @Override
    public String details() {
        return mode;
    }

    @Override
    public void onDisable() {
        target = null;
    }

    @Listen
    public void onWorldLoad(WorldLoadEvent event) {
        target = null;
    }

    @Listen
    public void onRender3D(Render3DEvent event) {
        long now = System.nanoTime();
        float delta = Math.min(0.1F, (now - lastFrame) / 1.0E9F);
        lastFrame = now;

        if (minecraft.mouse.x != 0 || minecraft.mouse.y != 0) lastMouseMove = System.currentTimeMillis();

        LocalClientPlayerEntity player = minecraft.player;
        if (player == null || findTarget(player.yaw, player.pitch) == null) {
            yawRemainder = pitchRemainder = 0.0F;
            return;
        }

        float[] goal = rotationTo(aimPoint(event.partialTicks, delta));
        float yawDiff = MathHelper.wrapDegrees(goal[0] - player.yaw);
        float pitchDiff = goal[1] - player.pitch;
        if (Math.hypot(yawDiff, pitchDiff) * 2.0D < fov[0]) return;

        float sensitivity = minecraft.options.mouseSensitivity * 0.6F + 0.2F;
        float scale = sensitivity * sensitivity * sensitivity * 8.0F;
        float gcd = scale * 0.15F;

        float yawTotal = step(yawDiff, horizontalSpeed, delta, mode) + yawRemainder;
        float pitchTotal = step(pitchDiff, verticalSpeed, delta, mode) + pitchRemainder;
        yawRemainder = yawTotal % gcd;
        pitchRemainder = pitchTotal % gcd;

        int yawCounts = mouseCounts(yawTotal, yawDiff, gcd);
        int pitchCounts = mouseCounts(pitchTotal, pitchDiff, gcd);
        if (yawCounts != 0 || pitchCounts != 0) player.updateLocalPlayerCamera(yawCounts * scale, -pitchCounts * scale);
    }

    private static int mouseCounts(float total, float diff, float gcd) {
        int limit = (int) (diff / gcd);
        return MathHelper.clamp((int) (total / gcd), Math.min(limit, 0), Math.max(limit, 0));
    }

    private static float step(float diff, float speed, float delta, String mode) {
        return switch (mode) {
            case "Lock-on" -> diff;
            case "Linear" -> {
                float limit = speed * 6.0F * delta * Math.min(1.0F, 0.4F + Math.abs(diff) / 15.0F);
                yield MathHelper.clamp(diff, -limit, limit);
            }
            default -> diff * (1.0F - (float) Math.pow(1.0F - speed / 100.0F, delta * 20.0F));
        };
    }

    private PlayerEntity findTarget(float yaw, float pitch) {
        if (!active()) return target = null;

        boolean single = targetMode.equals("Single");
        if (single && target != null && valid(target, yaw, pitch, increasedFov)) return target;

        String sort = single ? "Aim angle" : sortBy;
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity player : minecraft.world.players) {
            if (!valid(player, yaw, pitch, false)) continue;

            double score = score(player, sort, yaw, pitch);
            if (score < bestScore) {
                best = player;
                bestScore = score;
            }
        }
        return target = best;
    }

    private boolean active() {
        LocalClientPlayerEntity player = minecraft.player;
        if (player == null || minecraft.world == null || minecraft.screen != null || !player.isAlive()) return false;
        if (sprinting && !player.isSprinting()) return false;
        if (mousePressed && !Mouse.isButtonDown(0)) return false;
        if (mouseMoved && System.currentTimeMillis() - lastMouseMove > MOUSE_MOVE_WINDOW) return false;
        if (breakingBlocks && minecraft.interactionManager.isMiningBlock()) return false;
        return !holdingWeapon || Weapons.isWeapons(player.getItemInHand());
    }

    private boolean valid(PlayerEntity player, float yaw, float pitch, boolean anyFov) {
        LocalClientPlayerEntity self = minecraft.player;
        if (player == self || !player.isAlive() || !CombatUtils.isTarget(player)) return false;
        if (ignoreInvisibles && player.isInvisible()) return false;

        double distance = distance(player);
        if (distance < range[0] || distance > range[1]) return false;
        if (!anyFov && angle(player, yaw, pitch) * 2.0D > fov[1]) return false;
        return !notBehindBlocks || self.canSee(player);
    }

    private double score(PlayerEntity player, String sort, float yaw, float pitch) {
        return switch (sort) {
            case "Health" -> player.getHealth();
            case "Hurt time" -> player.damagedTimer;
            case "Aim angle" -> angle(player, yaw, pitch);
            default -> distance(player);
        };
    }

    private double distance(PlayerEntity player) {
        Vec3d eye = minecraft.player.getEyePosition(1.0F);
        Box box = player.getShape();
        double x = eye.x - MathHelper.clamp(eye.x, box.minX, box.maxX);
        double y = eye.y - MathHelper.clamp(eye.y, box.minY, box.maxY);
        double z = eye.z - MathHelper.clamp(eye.z, box.minZ, box.maxZ);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private double angle(PlayerEntity player, float yaw, float pitch) {
        Box box = player.getShape();
        float[] rotation = rotationTo(new Vec3d(
                (box.minX + box.maxX) / 2.0D, (box.minY + box.maxY) / 2.0D, (box.minZ + box.maxZ) / 2.0D));
        return Math.hypot(MathHelper.wrapDegrees(rotation[0] - yaw), rotation[1] - pitch);
    }

    private float[] rotationTo(Vec3d point) {
        Vec3d eye = minecraft.player.getEyePosition(1.0F);
        double x = point.x - eye.x;
        double y = point.y - eye.y;
        double z = point.z - eye.z;
        return new float[]{
                (float) Math.toDegrees(Math.atan2(z, x)) - 90.0F,
                (float) -Math.toDegrees(Math.atan2(y, Math.sqrt(x * x + z * z)))
        };
    }

    private Vec3d aimPoint(float tickDelta, float delta) {
        long now = System.currentTimeMillis();
        if (now >= nextNoise) {
            nextNoise = now + 200L + random.nextInt(300);
            for (int i = 0; i < 3; i++) noiseGoal[i] = random.nextFloat() * 2.0F - 1.0F;
        }
        for (int i = 0; i < 3; i++) noise[i] += (noiseGoal[i] - noise[i]) * Math.min(1.0F, delta * 4.0F);

        LocalClientPlayerEntity self = minecraft.player;
        double selfX = (self.x - self.prevX) * (tickDelta - 1.0F);
        double selfY = (self.y - self.prevY) * (tickDelta - 1.0F);
        double selfZ = (self.z - self.prevZ) * (tickDelta - 1.0F);
        double lead = tickDelta - 1.0F + predict / 100.0F * PREDICT_TICKS;
        Box box = target.getShape().moved(
                (target.x - target.prevX) * lead - selfX,
                (target.y - target.prevY) * (tickDelta - 1.0F) - selfY,
                (target.z - target.prevZ) * lead - selfZ);

        Vec3d eye = self.getEyePosition(1.0F);
        float spread = randomization / 100.0F;
        double x = axis(eye.x, box.minX, box.maxX, horizontalMultipoint, noise[0] * spread * 0.4F);
        double y = axis(eye.y, box.minY, box.maxY, verticalMultipoint, noise[1] * spread * 0.3F);
        double z = axis(eye.z, box.minZ, box.maxZ, horizontalMultipoint, noise[2] * spread * 0.4F);
        return new Vec3d(x, y, z);
    }

    private static double axis(double eye, double min, double max, float multipoint, float offset) {
        double center = (min + max) / 2.0D;
        double closest = MathHelper.clamp(eye, min + EDGE_INSET, max - EDGE_INSET);
        double point = center + (closest - center) * multipoint / 100.0F + (max - min) * offset;
        return MathHelper.clamp(point, min + EDGE_INSET, max - EDGE_INSET);
    }
}
