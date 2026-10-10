package org.codeberg.illusionclient.util;

import java.util.Random;

public final class ClickPattern {
	private final Random random = new Random();
	private float cps;
	private boolean randomize;
	private boolean simulateExhaust;
	private boolean butterfly;
	private boolean clicking;
	private float wait;
	private int burstSize;
	private int burstLeft;
	private float burstSpent;
	private int gapStart;
	private int gapSpread;
	private float singleChance;
	private float tripleChance;
	private int streak;
	private float streakCps;
	private int phase;
	private float phaseCps;
	private int delayStart;
	private int delaySpread;
	private float pauseChance;
	private int exhaustTicks;
	private float exhaust;

	public void tick() {
		if (exhaustTicks > 0) {
			exhaustTicks--;
		} else if (random.nextInt(120) == 0) {
			exhaustTicks = 6 + random.nextInt(14);
			exhaust = 1.3F + random.nextFloat() * 0.4F;
		}
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
			startBurst();
			wait = interval();
		}

		int clicks = 0;
		for (wait -= delta; wait <= 0.0F; wait += interval()) clicks++;
		return clicks;
	}

	private float period() {
		if (!randomize) return 1000.0F / cps;

		float period = halfPeriod() + halfPeriod();
		if (random.nextFloat() < pauseChance) period += 70 + random.nextInt(45);
		return period;
	}

	private float halfPeriod() {
		if (streak-- <= 0) {
			if (phase-- <= 0) {
				phase = 5 + random.nextInt(15);
				phaseCps = random.nextFloat() * 2.0F - 1.0F;
				delayStart = 380 + random.nextInt(40);
				delaySpread = 50 + random.nextInt(70);
				pauseChance = 0.02F + random.nextFloat() * 0.05F;
				gapStart = 15 + random.nextInt(15);
				gapSpread = 15 + random.nextInt(20);
				singleChance = 0.08F + random.nextFloat() * 0.25F;
				tripleChance = random.nextFloat() * 0.08F;
			}
			streak = 4 + random.nextInt(30);
			streakCps = Math.max(1.0F, cps + phaseCps + random.nextInt(6) - 3);
		}
		return (delayStart + random.nextInt(delaySpread)) / streakCps;
	}

	private float interval() {
		float ms = period();
		if (simulateExhaust && exhaustTicks > 0) ms *= exhaust;
		if (!butterfly) return ms;

		if (burstLeft > 0) {
			burstLeft--;
			float gap = randomize ? gapStart + random.nextFloat() * gapSpread : 30.0F;
			gap = Math.min(ms * 0.8F, gap);
			burstSpent += gap;
			return gap;
		}

		float rest = ms * burstSize - burstSpent;
		startBurst();
		return rest;
	}

	private void startBurst() {
		float roll = randomize ? random.nextFloat() : 1.0F;
		burstSize = roll < singleChance ? 1 : roll < singleChance + tripleChance ? 3 : 2;
		burstLeft = burstSize - 1;
		burstSpent = 0.0F;
	}
}
