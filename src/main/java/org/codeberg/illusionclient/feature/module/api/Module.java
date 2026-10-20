package org.codeberg.illusionclient.feature.module.api;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.feature.config.api.Option;

import java.lang.reflect.Field;
import java.util.List;

public abstract class Module {
	public final String name;
	public final String key = getClass().getSimpleName();
	public final Category category;
	public final Description description;
	public final List<Option> options = new ObjectArrayList<>();

	private int bind;
	private boolean enabled;

	public Module() {
		ModuleInfo info = getClass().getAnnotation(ModuleInfo.class);
		if (info == null) throw new IllegalStateException(getClass().getName() + " is missing @ModuleInfo");

		this.name = info.name();
		this.category = info.category();
		this.description = getClass().getAnnotation(Description.class);
		this.bind = info.bind();

		for (Field field : getClass().getDeclaredFields()) {
			Option option = Option.of(this, field);
			if (option == null) continue;

			option.link(options);
			options.add(option);
		}
	}

	protected static String number(float value) {
		return value == (int) value ? String.valueOf((int) value) : String.valueOf(value);
	}

	public final int getBind() {
		return bind;
	}

	public void setBind(int bind) {
		this.bind = bind;
	}

	public final boolean isEnabled() {
		return enabled;
	}

	public final void setEnabled(boolean enabled) {
		if (this.enabled == enabled) return;
		this.enabled = enabled;

		if (enabled) {
			IllusionClient.BUS.subscribe(this);
			onEnable();
		} else {
			IllusionClient.BUS.unsubscribe(this);
			onDisable();
		}
	}

	public final void toggle() {
		IllusionClient.STATS.toggled(this);
		setEnabled(!enabled);
	}

	public String details() {
		return null;
	}

	public void onEnable() {
	}

	public void onDisable() {
	}
}
