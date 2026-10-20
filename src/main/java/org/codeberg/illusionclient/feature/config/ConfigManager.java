package org.codeberg.illusionclient.feature.config;

import com.google.gson.*;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.config.api.*;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final String VERSION_KEY = "configVersion";
	private static final String STATS_KEY = "stats";
	private static final String GUI_KEY = "gui";
	private static final Map<String, String> RENAMED_LABELS = Map.of("Randomize Inventory", "Randomize Inventory Speed");
	private static final List<Consumer<JsonObject>> MIGRATIONS = List.of(
		ConfigManager::keyByFieldName,
		ConfigManager::keyByClassName
	);
	private static final int VERSION = MIGRATIONS.size();

	private final Path file = FabricLoader.getInstance().getConfigDir()
		.resolve(IllusionClient.ID).resolve("settings.json");

	public JsonObject gui = new JsonObject();

	private static void keyByFieldName(JsonObject root) {
		for (Module module : IllusionClient.MODULES.modules) {
			JsonElement entry = root.get(module.name);
			if (entry == null || !entry.isJsonObject()) continue;

			JsonElement settings = entry.getAsJsonObject().get("settings");
			if (settings == null || !settings.isJsonObject()) continue;

			JsonObject labelled = settings.getAsJsonObject();
			JsonObject keyed = new JsonObject();
			for (Option option : module.options) {
				JsonElement value = labelled.get(option.name);
				if (value == null && RENAMED_LABELS.containsKey(option.name)) {
					value = labelled.get(RENAMED_LABELS.get(option.name));
				}
				if (value != null) keyed.add(option.key, value);
			}
			entry.getAsJsonObject().add("settings", keyed);
		}
	}

	private static void keyByClassName(JsonObject root) {
		Map<String, JsonElement> entries = new HashMap<>();
		for (Module module : IllusionClient.MODULES.modules) {
			JsonElement entry = root.remove(module.name);
			if (entry != null) entries.put(module.key, entry);
		}
		entries.forEach(root::add);
	}

	private static int readVersion(JsonObject root) {
		JsonElement version = root.get(VERSION_KEY);
		if (version == null) return 0;

		int number = version.getAsInt();
		if (number < 0) throw new IllegalArgumentException("invalid " + VERSION_KEY + " " + number);
		return number;
	}

	private static void load(Module module, JsonObject entry) {
		JsonElement settings = entry.get("settings");
		if (settings != null && settings.isJsonObject()) {
			for (Option option : module.options) {
				JsonElement value = settings.getAsJsonObject().get(option.key);
				if (value != null) guarded(module, option.key, () -> option.set(read(value, option)));
			}
		}

		JsonElement bind = entry.get("bind");
		if (bind != null) {
			guarded(module, "bind", () -> {
				int key = bind.getAsInt();
				if (key < 0 || key >= Keyboard.KEYBOARD_SIZE) throw new IllegalArgumentException("unknown key " + key);
				module.setBind(key);
			});
		}

		JsonElement enabled = entry.get("enabled");
		if (enabled != null) guarded(module, "enabled", () -> module.setEnabled(readBoolean(enabled)));
	}

	private static void loadStats(JsonObject stats) {
		IllusionClient.STATS.previousPlaytime = readCount(stats, "playtime");
		IllusionClient.STATS.autoClicks = readCount(stats, "autoClicks");
		IllusionClient.STATS.bestCombo = readCount(stats, "bestCombo");
		readCounts(stats, "toggles", IllusionClient.STATS.toggles);
	}

	private static void readCounts(JsonObject stats, String key, Object2LongMap<String> counts) {
		JsonElement value = stats.get(key);
		if (value == null || !value.isJsonObject()) return;

		JsonObject object = value.getAsJsonObject();
		for (String entry : object.keySet()) counts.put(entry, readCount(object, entry));
	}

	private static JsonObject writeCounts(Object2LongMap<String> counts) {
		JsonObject object = new JsonObject();
		for (Object2LongMap.Entry<String> entry : counts.object2LongEntrySet()) object.addProperty(entry.getKey(), entry.getLongValue());
		return object;
	}

	private static long readCount(JsonObject stats, String key) {
		JsonElement value = stats.get(key);
		if (value == null) return 0;

		try {
			long count = value.getAsLong();
			if (count < 0) throw new IllegalArgumentException("negative count " + count);
			return count;
		} catch (RuntimeException e) {
			LOGGER.warn("Ignoring invalid '{}' in stats: {}", key, e.toString());
			return 0;
		}
	}

	private static void guarded(Module module, String key, Runnable action) {
		try {
			action.run();
		} catch (RuntimeException e) {
			LOGGER.warn("Ignoring invalid '{}' for module {}: {}", key, module.name, e.toString());
		}
	}

	private static Object read(JsonElement value, Option option) {
		if (option.info instanceof Check) return readBoolean(value);
		if (option.info instanceof ColorPicker) return new Color(value.getAsInt());

		if (option.info instanceof Slider slider) {
			float number = value.getAsFloat();
			if (Float.isNaN(number)) throw new IllegalArgumentException("not a number");
			return Math.clamp(number, slider.min(), slider.max());
		}

		if (option.info instanceof RangeSlider slider) {
			JsonArray array = value.getAsJsonArray();
			if (array.size() != 2) throw new IllegalArgumentException("expected two numbers, got " + value);

			float low = array.get(0).getAsFloat();
			float high = array.get(1).getAsFloat();
			if (Float.isNaN(low) || Float.isNaN(high)) throw new IllegalArgumentException("not a number");

			low = Math.clamp(low, slider.min(), slider.max());
			high = Math.clamp(high, slider.min(), slider.max());
			return new float[]{Math.min(low, high), Math.max(low, high)};
		}

		String choice = value.getAsString();
		if (!Arrays.asList(((Combo) option.info).options()).contains(choice)) {
			throw new IllegalArgumentException("unknown option " + choice);
		}
		return choice;
	}

	private static boolean readBoolean(JsonElement value) {
		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException("expected true or false, got " + value);
		}
		return value.getAsBoolean();
	}

	public synchronized void save() {
		JsonObject root = new JsonObject();
		root.addProperty(VERSION_KEY, VERSION);

		for (Module module : IllusionClient.MODULES.modules) {
			JsonObject settings = new JsonObject();
			for (Option option : module.options) {
				Object value = option.get();
				settings.add(option.key, value instanceof Color color
					? new JsonPrimitive(color.getRGB())
					: GSON.toJsonTree(value));
			}

			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			entry.addProperty("bind", module.getBind());
			entry.add("settings", settings);
			root.add(module.key, entry);
		}

		JsonObject stats = new JsonObject();
		stats.addProperty("playtime", IllusionClient.STATS.playtime());
		stats.addProperty("autoClicks", IllusionClient.STATS.autoClicks);
		stats.addProperty("bestCombo", IllusionClient.STATS.bestCombo);
		stats.add("toggles", writeCounts(IllusionClient.STATS.toggles));
		root.add(STATS_KEY, stats);
		root.add(GUI_KEY, gui);

		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}

			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException _) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save configuration to {}", file, e);
		}
	}

	public synchronized void load() {
		if (!Files.exists(file)) return;

		JsonObject root;
		int version;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			root = JsonParser.parseReader(reader).getAsJsonObject();
			version = readVersion(root);
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Failed to read {}, using defaults and keeping a copy at {}", file, backup("broken"), e);
			return;
		}

		if (version > VERSION) {
			LOGGER.warn("{} was written by a newer version ({} > {}), loading what still matches and keeping a copy at {}",
				file, version, VERSION, backup("v" + version));
		} else if (version < VERSION) {
			Path copy = backup("v" + version);
			try {
				for (int i = version; i < VERSION; i++) MIGRATIONS.get(i).accept(root);
			} catch (RuntimeException e) {
				LOGGER.error("Failed to migrate {} from version {}, using defaults and keeping a copy at {}", file, version, copy, e);
				return;
			}
			LOGGER.info("Migrated {} from version {} to {}, the original is at {}", file, version, VERSION, copy);
		}

		for (Module module : IllusionClient.MODULES.modules) {
			JsonElement entry = root.get(module.key);
			if (entry != null && entry.isJsonObject()) load(module, entry.getAsJsonObject());
		}

		JsonElement stats = root.get(STATS_KEY);
		if (stats != null && stats.isJsonObject()) loadStats(stats.getAsJsonObject());

		JsonElement layout = root.get(GUI_KEY);
		if (layout != null && layout.isJsonObject()) gui = layout.getAsJsonObject();

		if (version < VERSION) save();
	}

	private Path backup(String suffix) {
		Path copy = file.resolveSibling(file.getFileName() + "." + suffix);
		try {
			Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			LOGGER.error("Failed to back up {}", file, e);
		}
		return copy;
	}
}
