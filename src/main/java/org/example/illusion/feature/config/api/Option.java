package org.example.illusion.feature.config.api;

import java.awt.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

public final class Option {
	public final String name;
	public final String key;
	public final Annotation info;
	private final Object owner;
	private final Field field;

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
