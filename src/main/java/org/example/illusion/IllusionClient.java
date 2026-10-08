package org.example.illusion;

import io.github.nevalackin.radbus.PubSub;
import org.example.illusion.event.Event;
import org.example.illusion.feature.config.ConfigManager;
import org.example.illusion.feature.module.ModuleManager;

public final class IllusionClient {
	public static final String ID = "illusion";
	public static final String NAME = "Illusion Client";

	public static final PubSub<Event> BUS = PubSub.newInstance(System.err::println);
	public static ModuleManager MODULES;
	public static ConfigManager CONFIG;

	private IllusionClient() {
	}

	public static void init() {
		MODULES = new ModuleManager();
		CONFIG = new ConfigManager();

		CONFIG.load();
		Runtime.getRuntime().addShutdownHook(new Thread(CONFIG::save));
	}
}
