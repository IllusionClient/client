package org.example.illusion.feature.module;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.example.illusion.IllusionClient;
import org.example.illusion.event.KeyPressEvent;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.client.AntiBot;
import org.example.illusion.feature.module.client.ClickGuiModule;
import org.example.illusion.feature.module.client.Teams;
import org.example.illusion.feature.module.combat.AimAssist;
import org.example.illusion.feature.module.combat.AutoClicker;
import org.example.illusion.feature.module.combat.NoHitDelay;
import org.example.illusion.feature.module.combat.Weapons;
import org.example.illusion.feature.module.movement.NullMove;
import org.example.illusion.feature.module.movement.NoJumpDelay;
import org.example.illusion.feature.module.movement.Sprint;
import org.example.illusion.feature.module.player.BridgeAssist;
import org.example.illusion.feature.module.player.FastPlace;
import org.example.illusion.feature.module.visual.Esp;
import org.example.illusion.feature.module.visual.ModOverlay;

import java.util.Arrays;
import java.util.List;

public final class ModuleManager {
	public final List<Module> modules = Arrays.asList(
		new AntiBot(),
		new ClickGuiModule(),
		new Teams(),
		new AimAssist(),
		new AutoClicker(),
		new NoHitDelay(),
		new Weapons(),
		new NoJumpDelay(),
		new NullMove(),
		new Sprint(),
		new BridgeAssist(),
		new FastPlace(),
		new Esp(),
		new ModOverlay()
	);
	private final Reference2ObjectMap<Class<?>, Module> byType = new Reference2ObjectOpenHashMap<>(modules.size());

	public ModuleManager() {
		for (Module module : modules) byType.put(module.getClass(), module);
		IllusionClient.BUS.subscribe(this);
	}

	public <T extends Module> T get(Class<T> type) {
		Module module = byType.get(type);
		if (module == null) throw new IllegalArgumentException("Unregistered module " + type.getName());
		return type.cast(module);
	}

	@Listen
	public void onKeyPress(KeyPressEvent event) {
		if (event.key() == 0 || !event.down()) return;

		for (Module module : modules) {
			if (module.getBind() == event.key()) module.toggle();
		}
	}
}
