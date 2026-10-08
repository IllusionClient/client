package org.example.illusion;

import io.github.nevalackin.radbus.PubSub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.illusion.event.Event;
import org.example.illusion.feature.config.ConfigManager;
import org.example.illusion.feature.module.ModuleManager;

public final class IllusionClient {
	public static final String ID = "illusion";
	public static final String NAME = "Illusion Client";

	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	public static final PubSub<Event> BUS = PubSub.newInstance(LOGGER::error);
	public static ModuleManager MODULES;
	public static ConfigManager CONFIG;

	public static void init() {
		MODULES = new ModuleManager();
		CONFIG = new ConfigManager();

		CONFIG.load();
		Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::save));
	}
}
