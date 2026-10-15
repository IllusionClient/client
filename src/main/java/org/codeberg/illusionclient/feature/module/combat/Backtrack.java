package org.codeberg.illusionclient.feature.module.combat;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.living.player.LocalClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityS2CPacket;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.PacketReceiveEvent;
import org.codeberg.illusionclient.event.PlayerTickEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.RangeSlider;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.util.CombatUtils;
import org.codeberg.illusionclient.util.GhostPlayer;
import org.codeberg.illusionclient.util.PacketQueue;

@ModuleInfo(name = "Backtrack", category = Category.COMBAT)
@Description("Lags players leaving your range for extra hits.")
public class Backtrack extends Module {
	private static final int MILLIS_PER_TICK = 50;
	private static final double POSITION_SCALE = 32.0D;
	private final Minecraft minecraft = Minecraft.getInstance();
	private final PacketQueue packets = new PacketQueue();

	@Description("Only lags players within this distance.")
	@RangeSlider(value = "Target Distance", min = 0.0F, max = 8.0F)
	public float[] targetDistance = {1.0F, 4.0F};

	@Description("The longest the target is held back.")
	@Slider(value = "Maximum Delay (ms)", min = 0.0F, max = 500.0F)
	public float maximumDelay = 200.0F;

	@Description("Only lags players hit within this time. 500 is unlimited.")
	@Slider(value = "Maximum Hurt Time (ms)", min = 0.0F, max = 500.0F)
	public float maximumHurtTime = 500.0F;

	@Description("Cooldown before lagging again.")
	@Slider(value = "Cooldown (s)", min = 0.0F, max = 5.0F)
	public float cooldown = 0.0F;

	@Description("Stops lagging as soon as you take knockback.")
	@Check("Disable On Hit")
	public boolean disableOnHit = true;

	@Description("Replies to the server instantly so ping stays low.")
	@Check("Ping Fix")
	public boolean pingFix = false;

	@Description("Shows the target's real server position.")
	@Check("Show Ghost")
	public boolean showGhost = true;

	@Description("How visible the ghost is.")
	@Requires("showGhost")
	@Slider(value = "Ghost Opacity", min = 0.0F, max = 100.0F)
	public float ghostOpacity = 50.0F;

	private PlayerEntity target;
	private GhostPlayer ghost;
	private boolean lagging;
	private long readyAt;

	public static boolean holdsKeepAlives() {
		Backtrack backtrack = IllusionClient.MODULES.get(Backtrack.class);
		return backtrack.isEnabled() && !backtrack.pingFix;
	}

	@Override
	public String details() {
		return number(maximumDelay) + " ms";
	}

	@Override
	public void onDisable() {
		target = null;
		stop();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		target = null;
		ghost = null;
	}

	@Listen
	public void onPacketReceive(PacketReceiveEvent event) {
		if (lagging && disableOnHit && isKnockback(event.packet())) stop();
		if (!lagging && packets.isEmpty()) return;

		event.cancelled = true;
		packets.add(event.packet(), event.listener());
		if (ghost != null) ghost.handle(event.packet());
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		if (showGhost && ghost != null) ghost.render(event.partialTicks(), ghostOpacity / 100.0F);
	}

	@Listen
	public void onPlayerTick(PlayerTickEvent event) {
		if (minecraft.world == null || minecraft.player == null) {
			target = null;
			ghost = null;
			lagging = false;
			packets.clear();
			return;
		}

		if (!lagging && System.currentTimeMillis() >= readyAt) {
			target = findTarget();
			lagging = target != null;
			if (lagging) ghost = new GhostPlayer(target);
		}

		if (lagging && ghost != null && shouldLag(target)) {
			ghost.tick();
			packets.release((long) maximumDelay);
		} else {
			stop();
		}
	}

	private void stop() {
		if (lagging) readyAt = System.currentTimeMillis() + (long) (cooldown * 1000.0F);
		lagging = false;
		ghost = null;
		packets.release(0L);
	}

	private boolean isKnockback(Packet<?> packet) {
		LocalClientPlayerEntity player = minecraft.player;
		return player != null && packet instanceof EntityVelocityS2CPacket velocity && velocity.getId() == player.getNetworkId();
	}

	private PlayerEntity findTarget() {
		PlayerEntity best = null;
		double bestDistance = Double.MAX_VALUE;
		for (PlayerEntity player : minecraft.world.players) {
			if (player == minecraft.player || !shouldLag(player)) continue;

			double distance = CombatUtils.distanceTo(player);
			if (distance < bestDistance) {
				best = player;
				bestDistance = distance;
			}
		}
		return best;
	}

	private boolean shouldLag(PlayerEntity player) {
		if (player == null || !player.isAlive() || !CombatUtils.isTarget(player)) return false;
		if (player.damagedTimer * MILLIS_PER_TICK > maximumHurtTime) return false;

		double distance = CombatUtils.distanceTo(player);
		if (distance < targetDistance[0] || distance > targetDistance[1]) return false;
		return serverDistanceTo(player) > distance;
	}

	private double serverDistanceTo(PlayerEntity player) {
		Entity server = player == target && ghost != null ? ghost : player;
		return CombatUtils.distanceTo(player.getShape().moved(
			server.lastKnownX / POSITION_SCALE - player.x,
			server.lastKnownY / POSITION_SCALE - player.y,
			server.lastKnownZ / POSITION_SCALE - player.z));
	}
}
