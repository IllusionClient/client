package org.codeberg.illusionclient;

import io.github.nevalackin.radbus.PubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codeberg.illusionclient.event.Event;
import org.codeberg.illusionclient.feature.config.ConfigManager;
import org.codeberg.illusionclient.feature.input.InputManager;
import org.codeberg.illusionclient.feature.lag.LagManager;
import org.codeberg.illusionclient.feature.module.ModuleManager;
import org.codeberg.illusionclient.feature.stats.Statistics;

public final class IllusionClient {
	public static final String ID = "illusion";
	public static final String NAME = "Illusion Client";

	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	public static final PubSub<Event> BUS = PubSub.newInstance(LOGGER::error);
	public static ModuleManager MODULES;
	public static ConfigManager CONFIG;
	public static InputManager INPUT;
	public static LagManager LAG;
	public static Statistics STATS;

	public static void init() {
		INPUT = new InputManager();
		STATS = new Statistics();
		LAG = new LagManager();
		MODULES = new ModuleManager();
		CONFIG = new ConfigManager();

		CONFIG.load();
		BUS.subscribe(STATS);
		Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::save));
	}
}
