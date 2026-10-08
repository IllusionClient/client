package org.example.illusion.feature.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.illusion.IllusionClient;
import org.example.illusion.feature.config.api.*;
import org.example.illusion.feature.module.api.Module;
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

public final class ConfigManager {
	private static final Logger LOGGER = LogManager.getLogger(ConfigManager.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path file = FabricLoader.getInstance().getConfigDir()
		.resolve(IllusionClient.ID).resolve("settings.json");

	private static void load(Module module, JsonObject entry) {
		JsonElement settings = entry.get("settings");
		if (settings != null && settings.isJsonObject()) {
			for (Option option : module.options) {
				JsonElement value = settings.getAsJsonObject().get(option.name);
				if (value != null) guarded(module, option.name, () -> option.set(read(value, option)));
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
			return Math.min(slider.max(), Math.max(slider.min(), number));
		}

		if (option.info instanceof RangeSlider slider) {
			JsonArray array = value.getAsJsonArray();
			if (array.size() != 2) throw new IllegalArgumentException("expected two numbers, got " + value);

			float low = array.get(0).getAsFloat();
			float high = array.get(1).getAsFloat();
			if (Float.isNaN(low) || Float.isNaN(high)) throw new IllegalArgumentException("not a number");

			low = Math.min(slider.max(), Math.max(slider.min(), low));
			high = Math.min(slider.max(), Math.max(slider.min(), high));
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

		for (Module module : IllusionClient.MODULES.modules) {
			JsonObject settings = new JsonObject();
			for (Option option : module.options) {
				Object value = option.get();
				settings.add(option.name, value instanceof Color
					? new JsonPrimitive(((Color) value).getRGB())
					: GSON.toJsonTree(value));
			}

			JsonObject entry = new JsonObject();
			entry.addProperty("enabled", module.isEnabled());
			entry.addProperty("bind", module.getBind());
			entry.add("settings", settings);
			root.add(module.name, entry);
		}

		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.createDirectories(file.getParent());
			try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}

			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to save configuration to {}", file, e);
		}
	}

	public synchronized void load() {
		if (!Files.exists(file)) return;

		JsonObject root;
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			root = new JsonParser().parse(reader).getAsJsonObject();
		} catch (IOException | RuntimeException e) {
			Path broken = file.resolveSibling(file.getFileName() + ".broken");
			LOGGER.error("Failed to read {}, using defaults and keeping a copy at {}", file, broken, e);
			try {
				Files.copy(file, broken, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException copyError) {
				LOGGER.error("Failed to back up {}", file, copyError);
			}
			return;
		}

		for (Module module : IllusionClient.MODULES.modules) {
			JsonElement entry = root.get(module.name);
			if (entry != null && entry.isJsonObject()) load(module, entry.getAsJsonObject());
		}
	}
}
