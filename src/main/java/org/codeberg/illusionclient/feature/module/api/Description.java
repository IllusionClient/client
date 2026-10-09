package org.codeberg.illusionclient.feature.module.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
	String value();

	Type type() default Type.INFO;

	enum Type {
		INFO(0xFFAAAAAA),
		WARNING(0xFFFFC107);

		public final int color;

		Type(int color) {
			this.color = color;
		}
	}
}
