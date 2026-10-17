package org.codeberg.illusionclient.util;

import net.minecraft.util.math.MathHelper;

import java.util.Random;

public final class ClickPattern {
	private final Random random = new Random();
	private final Drift phaseCps = new Drift(75_000L);
	private final Drift streakCps = new Drift(18_000L);
	private final Drift jitterCps = new Drift(3_500L);
	private final Drift deviation = new Drift(25_000L);
	private final Drift gapSpread = new Drift(12_000L);
	private final Drift singleChance = new Drift(30_000L);
	private final Drift exhaustChance = new Drift(240_000L);
	private final Drift exhaustLength = new Drift(60_000L);
	private final Drift liftShare = new Drift(40_000L);
	private float cps;
	private boolean randomize;
	private boolean simulateExhaust;
	private boolean butterfly;
	private boolean clicking;
	private float wait;
	private boolean lifted;
	private float lift;
	private int presses;
	private int burstLeft;
	private float gap;
	private int exhaustClicks;
	private float exhaust;
	private float lastYaw;
	private float lastPitch;
	private float activity;

	public void tick(float yaw, float pitch) {
		float turned = (float) Math.hypot(MathHelper.wrapDegrees(yaw - lastYaw), pitch - lastPitch);
		lastYaw = yaw;
		lastPitch = pitch;
		activity = activity * 0.6F + Math.min(turned, 30.0F) / 25.0F * 0.4F;
	}

	public void stop() {
		clicking = false;
	}

	public int advance(float delta, float cps, boolean randomize, boolean simulateExhaust, boolean butterfly) {
		this.cps = cps;
		this.randomize = randomize;
		this.simulateExhaust = simulateExhaust;
		this.butterfly = butterfly;

		if (!clicking) {
			clicking = true;
			burstLeft = 0;
			exhaustClicks = 0;
			lifted = false;
			wait = split(interval());
		}

		int edges = 0;
		presses = 0;
		for (wait -= delta; wait <= 0.0F; wait += edge()) edges++;
		return edges;
	}

	public int presses() {
		return presses;
	}

	private float edge() {
		lifted = !lifted;
		if (lifted) return lift;
		presses++;
		return split(interval());
	}

	private float split(float ms) {
		float share = randomize ? liftShare.range(System.currentTimeMillis(), 0.3F, 0.5F) + 0.06F * gaussian() : 0.5F;
		lift = ms * share;
		return ms - lift;
	}

	private float period(long now) {
		if (!randomize) return 1000.0F / cps;

		float drift = 1.0F + phaseCps.signed(now) * 0.1F + streakCps.signed(now) * 0.07F + jitterCps.signed(now) * 0.04F;
		return 1000.0F / (cps * drift * (1.0F + gaussian() * deviation.range(now, 0.22F, 0.34F)));
	}

	private float interval() {
		long now = System.currentTimeMillis();
		float ms = period(now);
		if (!butterfly) return ms + slowdown(now);

		if (burstLeft > 0) {
			burstLeft--;
			return gap;
		}

		float slowdown = slowdown(now);
		startBurst(now, slowdown > 0.0F);
		return (burstLeft > 0 ? 2.0F * ms - gap : ms) + slowdown;
	}

	private void startBurst(long now, boolean exhausted) {
		gap = randomize ? gapSpread.range(now, 22.0F, 40.0F) : 30.0F;
		boolean single = exhausted || randomize && random.nextFloat() < singleChance.range(now, 0.03F, 0.07F);
		burstLeft = single ? 0 : 1;
	}

	private float slowdown(long now) {
		if (exhaustClicks > 0) {
			exhaustClicks--;
			return exhaust * (1.0F + 0.35F * gaussian());
		}
		if (!simulateExhaust || random.nextFloat() >= exhaustChance.range(now, 0.012F, 0.035F) + activity * 0.5F) {
			return 0.0F;
		}

		float keep = exhaustLength.range(now, 0.1F, 0.25F);
		while (exhaustClicks < 3 && random.nextFloat() < keep) exhaustClicks++;
		exhaust = 18.0F + random.nextFloat() * 22.0F;
		return exhaust * (1.0F + 0.35F * gaussian());
	}

	private float gaussian() {
		return (random.nextFloat() + random.nextFloat() + random.nextFloat()) / 1.5F - 1.0F;
	}

	private final class Drift {
		private final long length;
		private float from;
		private float to;
		private long start;
		private long segment;

		private Drift(long length) {
			this.length = length;
		}

		private float range(long now, float min, float max) {
			return min + (max - min) * sample(now);
		}

		private float signed(long now) {
			return sample(now) * 2.0F - 1.0F;
		}

		private float sample(long now) {
			if (segment == 0L || now - start > segment + length * 4L) {
				to = random.nextFloat();
				start = now;
				next();
			}
			while (now - start >= segment) {
				start += segment;
				next();
			}
			float t = (float) (now - start) / segment;
			return from + (to - from) * t * t * (3.0F - 2.0F * t);
		}

		private void next() {
			from = to;
			float jump = (random.nextBoolean() ? 1.0F : -1.0F) * (0.25F + random.nextFloat() * 0.5F);
			to = from + jump;
			if (to < 0.0F || to > 1.0F) to = Math.clamp(from - jump, 0.0F, 1.0F);
			segment = (long) (length * (0.6F + random.nextFloat() * 0.8F));
		}
	}
}
