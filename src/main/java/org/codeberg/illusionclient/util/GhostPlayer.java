package org.codeberg.illusionclient.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.RemoteClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.platform.GLX;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDataS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEquipmentS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityEventS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityHeadAnglesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityMoveS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTeleportS2CPacket;
import net.minecraft.scoreboard.team.AbstractTeam;
import org.codeberg.illusionclient.mixin.RemoteClientPlayerEntityAccessor;

public final class GhostPlayer extends RemoteClientPlayerEntity {
	private static final double POSITION_SCALE = 32.0D;
	private static final float ANGLE_SCALE = 360.0F / 256.0F;
	private static final int LERP_STEPS = 3;
	private static final double TELEPORT_TOLERANCE = 0.03125D;
	private static final double TELEPORT_TOLERANCE_Y = 0.015625D;
	private static final int EQUIPMENT_SLOTS = 5;
	private static final int HURT_EVENT = 2;
	private final PlayerEntity player;

	public GhostPlayer(PlayerEntity player) {
		super(player.world, player.getGameProfile());
		this.player = player;

		copyPositionAndRotationFrom(player);
		lastX = player.lastX;
		lastY = player.lastY;
		lastZ = player.lastZ;
		prevX = player.prevX;
		prevY = player.prevY;
		prevZ = player.prevZ;
		lastYaw = player.lastYaw;
		lastPitch = player.lastPitch;
		lastKnownX = player.lastKnownX;
		lastKnownY = player.lastKnownY;
		lastKnownZ = player.lastKnownZ;
		onGround = player.onGround;
		ticks = player.ticks;

		headYaw = player.headYaw;
		lastHeadYaw = player.lastHeadYaw;
		bodyYaw = player.bodyYaw;
		lastBodyYaw = player.lastBodyYaw;
		walkAnimationSpeed = player.walkAnimationSpeed;
		lastWalkAnimationSpeed = player.lastWalkAnimationSpeed;
		walkAnimationProgress = player.walkAnimationProgress;
		armSwinging = player.armSwinging;
		armSwingingTicks = player.armSwingingTicks;
		attackAnimationProgress = player.attackAnimationProgress;
		lastAttackAnimationProgress = player.lastAttackAnimationProgress;
		damagedTimer = player.damagedTimer;
		damagedTime = player.damagedTime;
		damagedSwingDir = player.damagedSwingDir;
		walkDistance = player.walkDistance;
		lastWalkDistance = player.lastWalkDistance;
		bob = player.bob;
		lastBob = player.lastBob;
		capeX = player.capeX;
		capeY = player.capeY;
		capeZ = player.capeZ;
		lastCapeX = player.lastCapeX;
		lastCapeY = player.lastCapeY;
		lastCapeZ = player.lastCapeZ;

		getSyncedData().update(player.getSyncedData().getAll());
		for (int slot = 0; slot < EQUIPMENT_SLOTS; slot++) setEquipment(slot, player.getEquipment(slot));

		if (player instanceof RemoteClientPlayerEntityAccessor lerp) {
			lerpPositionAndAngles(lerp.illusion$lerpX(), lerp.illusion$lerpY(), lerp.illusion$lerpZ(),
				(float) lerp.illusion$lerpYaw(), (float) lerp.illusion$lerpPitch(), lerp.illusion$lerpSteps(), false);
		} else {
			lerpToServerPosition(yaw, pitch);
		}
	}

	public void handle(Packet<?> packet) {
		if (packet instanceof EntityMoveS2CPacket move && move.getEntity(world) == player) {
			lastKnownX += move.getDx();
			lastKnownY += move.getDy();
			lastKnownZ += move.getDz();
			if (move.hasAngles()) lerpToServerPosition(move.getYaw() * ANGLE_SCALE, move.getPitch() * ANGLE_SCALE);
			else lerpToServerPosition(yaw, pitch);
			onGround = move.getOnGround();
		} else if (packet instanceof EntityTeleportS2CPacket teleport && teleport.getId() == player.getNetworkId()) {
			lastKnownX = teleport.getX();
			lastKnownY = teleport.getY();
			lastKnownZ = teleport.getZ();
			float teleportYaw = teleport.getYaw() * ANGLE_SCALE;
			float teleportPitch = teleport.getPitch() * ANGLE_SCALE;
			if (isNearServerPosition()) lerpPositionAndAngles(x, y, z, teleportYaw, teleportPitch, LERP_STEPS, true);
			else lerpToServerPosition(teleportYaw, teleportPitch);
			onGround = teleport.getOnGround();
		} else if (packet instanceof EntityHeadAnglesS2CPacket head && head.getEntity(world) == player) {
			setHeadYaw(head.getHeadYaw() * ANGLE_SCALE);
		} else if (packet instanceof EntityAnimationS2CPacket animation && animation.getId() == player.getNetworkId()) {
			if (animation.getAction() == 0) swingArm();
			else if (animation.getAction() == 1) animateDamage();
		} else if (packet instanceof EntityEventS2CPacket event && event.getEntity(world) == player) {
			if (event.getEvent() == HURT_EVENT) {
				walkAnimationSpeed = 1.5F;
				damagedTimer = damagedTime = 10;
				damagedSwingDir = 0.0F;
			}
		} else if (packet instanceof EntityDataS2CPacket data && data.getId() == player.getNetworkId()) {
			if (data.getDataEntries() != null) getSyncedData().update(data.getDataEntries());
		} else if (packet instanceof EntityEquipmentS2CPacket equipment && equipment.getId() == player.getNetworkId()) {
			setEquipment(equipment.getEquipmentSlot(), equipment.getItem());
		}
	}

	@Override
	public void tick() {
		saveLastTickState();
		super.tick();
	}

	public void render(float tickDelta, float opacity) {
		Minecraft minecraft = Minecraft.getInstance();
		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		double renderX = prevX + (x - prevX) * tickDelta - dispatcher.cameraX;
		double renderY = prevY + (y - prevY) * tickDelta - dispatcher.cameraY;
		double renderZ = prevZ + (z - prevZ) * tickDelta - dispatcher.cameraZ;
		float renderYaw = lastYaw + (yaw - lastYaw) * tickDelta;
		int light = getLightLevel(tickDelta);

		minecraft.gameRenderer.enableLightMap();
		Lighting.turnOn();
		GLX.multiTexCoord2f(GLX.GL_TEXTURE1, light % 65536, light / 65536);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		EntityRenderer<GhostPlayer> renderer = dispatcher.getRenderer(this);
		GlStateManager.colorMask(false, false, false, false);
		renderer.render(this, renderX, renderY, renderZ, renderYaw, tickDelta);
		GlStateManager.colorMask(true, true, true, true);

		RenderUtils.beginConstantAlpha(opacity);
		renderer.render(this, renderX, renderY, renderZ, renderYaw, tickDelta);
		RenderUtils.endConstantAlpha();

		Lighting.turnOff();
		minecraft.gameRenderer.disableLightMap();
	}

	@Override
	public boolean isInvisibleTo(PlayerEntity viewer) {
		return true;
	}

	@Override
	public AbstractTeam getScoreboardTeam() {
		return null;
	}

	private void saveLastTickState() {
		prevX = x;
		prevY = y;
		prevZ = z;
		lastYaw = yaw;
		lastPitch = pitch;
		ticks++;
	}

	private boolean isNearServerPosition() {
		return Math.abs(x - lastKnownX / POSITION_SCALE) < TELEPORT_TOLERANCE
			&& Math.abs(y - lastKnownY / POSITION_SCALE) < TELEPORT_TOLERANCE_Y
			&& Math.abs(z - lastKnownZ / POSITION_SCALE) < TELEPORT_TOLERANCE;
	}

	private void lerpToServerPosition(float yaw, float pitch) {
		lerpPositionAndAngles(lastKnownX / POSITION_SCALE, lastKnownY / POSITION_SCALE, lastKnownZ / POSITION_SCALE,
			yaw, pitch, LERP_STEPS, false);
	}
}
