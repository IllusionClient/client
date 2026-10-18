package org.codeberg.illusionclient.feature.module;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.Platform;
import org.codeberg.illusionclient.event.KeyPressEvent;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.client.AntiBot;
import org.codeberg.illusionclient.feature.module.client.ClickGuiModule;
import org.codeberg.illusionclient.feature.module.client.Teams;
import org.codeberg.illusionclient.feature.module.client.TestModule;
import org.codeberg.illusionclient.feature.module.combat.AimAssist;
import org.codeberg.illusionclient.feature.module.combat.AutoClicker;
import org.codeberg.illusionclient.feature.module.combat.Backtrack;
import org.codeberg.illusionclient.feature.module.combat.NoHitDelay;
import org.codeberg.illusionclient.feature.module.combat.Weapons;
import org.codeberg.illusionclient.feature.module.movement.NullMove;
import org.codeberg.illusionclient.feature.module.movement.NoJumpDelay;
import org.codeberg.illusionclient.feature.module.movement.Sprint;
import org.codeberg.illusionclient.feature.module.player.BridgeAssist;
import org.codeberg.illusionclient.feature.module.player.FastPlace;
import org.codeberg.illusionclient.feature.module.visual.AntiDebuff;
import org.codeberg.illusionclient.feature.module.visual.Esp;
import org.codeberg.illusionclient.feature.module.visual.ModOverlay;
import org.codeberg.illusionclient.feature.ui.ClickGui;

import java.util.Arrays;
import java.util.List;

public final class ModuleManager {
	public final List<Module> modules = builtIn();
	private final Reference2ObjectMap<Class<?>, Module> byType = new Reference2ObjectOpenHashMap<>();

	public ModuleManager() {
		if (Platform.isDevelopmentEnvironment()) modules.add(new TestModule());
		for (Module module : modules) byType.put(module.getClass(), module);
		IllusionClient.BUS.subscribe(this);
	}

	public static List<Module> builtIn() {
		return new ObjectArrayList<>(Arrays.asList(
			new AntiBot(),
			new ClickGuiModule(),
			new Teams(),
			new AimAssist(),
			new AutoClicker(),
			new Backtrack(),
			new NoHitDelay(),
			new Weapons(),
			new NoJumpDelay(),
			new NullMove(),
			new Sprint(),
			new BridgeAssist(),
			new FastPlace(),
			new AntiDebuff(),
			new Esp(),
			new ModOverlay()
		));
	}

	public <T extends Module> T get(Class<T> type) {
		Module module = byType.get(type);
		if (module == null) throw new IllegalArgumentException("Unregistered module " + type.getName());
		return type.cast(module);
	}

	@Listen
	public void onKeyPress(KeyPressEvent event) {
		if (event.key() == 0 || !event.down()) return;

		Screen screen = Minecraft.getInstance().screen;
		if (screen != null) {
			ClickGuiModule clickGui = get(ClickGuiModule.class);
			if (screen instanceof ClickGui && clickGui.getBind() == event.key()) clickGui.toggle();
			return;
		}

		for (Module module : modules) {
			if (module.getBind() == event.key()) module.toggle();
		}
	}
}
