package org.codeberg.illusionclient.feature.config.api;

import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

public final class Option {
	public final String name;
	public final String key;
	public final Annotation info;
	private final Object owner;
	private final Field field;
	private Option required;
	private List<String> requiredValues;

	private Option(Object owner, Field field, Annotation info, String name, Class<?> type) {
		if (field.getType() != type) {
			throw new IllegalStateException("@" + info.annotationType().getSimpleName() + " on "
				+ field.getDeclaringClass().getName() + "." + field.getName()
				+ " needs a " + type.getSimpleName() + " field, not " + field.getType().getSimpleName());
		}

		this.name = name;
		this.key = field.getName();
		this.info = info;
		this.owner = owner;
		this.field = field;
		field.setAccessible(true);
	}

	public static Option of(Object owner, Field field) {
		Check check = field.getAnnotation(Check.class);
		if (check != null) return new Option(owner, field, check, check.value(), boolean.class);

		Slider slider = field.getAnnotation(Slider.class);
		if (slider != null) return new Option(owner, field, slider, slider.value(), float.class);

		RangeSlider range = field.getAnnotation(RangeSlider.class);
		if (range != null) return new Option(owner, field, range, range.value(), float[].class);

		Combo combo = field.getAnnotation(Combo.class);
		if (combo != null) return new Option(owner, field, combo, combo.value(), String.class);

		ColorPicker picker = field.getAnnotation(ColorPicker.class);
		if (picker != null) return new Option(owner, field, picker, picker.value(), Color.class);

		return null;
	}

	public void link(List<Option> earlier) {
		Requires requires = field.getAnnotation(Requires.class);
		if (requires == null) return;

		requiredValues = List.of(requires.is());
		for (Option option : earlier) {
			if (option.key.equals(requires.value())) required = option;
		}

		boolean valid = required != null && (requiredValues.isEmpty()
			? required.info instanceof Check
			: required.info instanceof Combo combo && List.of(combo.options()).containsAll(requiredValues));
		if (!valid) {
			throw new IllegalStateException("@Requires on " + field.getDeclaringClass().getName() + "." + field.getName()
				+ " needs " + requires.value() + " to be an earlier @Check, or an earlier @Combo offering " + requiredValues);
		}
	}

	public boolean visible() {
		if (required == null) return true;
		if (!required.visible()) return false;
		return requiredValues.isEmpty() ? required.getBoolean() : requiredValues.contains(required.<String>get());
	}

	@SuppressWarnings("unchecked")
	public <T> T get() {
		try {
			return (T) field.get(owner);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public boolean getBoolean() {
		try {
			return field.getBoolean(owner);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public float getFloat() {
		try {
			return field.getFloat(owner);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public void set(Object value) {
		try {
			field.set(owner, value);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
