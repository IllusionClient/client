package org.codeberg.illusionclient.docs;

import org.codeberg.illusionclient.feature.config.api.Option;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.module.ModuleManager;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class DocsGenerator {
	static void main(String[] args) throws IOException {
		Path output = Path.of(args[0]);
		String version = args[1];
		String channel = args[2];

		Map<Category, List<Module>> byCategory = new TreeMap<>();
		for (Module module : ModuleManager.builtIn()) {
			if (module.description == null) {
				throw new IllegalStateException(module.getClass().getName() + " is missing @Description");
			}
			byCategory.computeIfAbsent(module.category, _ -> new ArrayList<>()).add(module);
		}
		byCategory.values().forEach(modules -> modules.sort(Comparator.comparing(module -> module.name)));

		StringBuilder nav = new StringBuilder();
		StringBuilder body = new StringBuilder();
		byCategory.forEach((category, modules) -> {
			String title = title(category);
			String id = category.name().toLowerCase(Locale.ROOT);
			nav.append("<div class=\"frame\">\n<a class=\"frame-head\" href=\"#").append(id).append("\">").append(title).append("</a>\n");
			body.append("<section class=\"category\" id=\"").append(id).append("\">\n")
				.append("<h2>").append(title).append("</h2>\n");
			for (Module module : modules) {
				nav.append("<a href=\"#").append(module.key).append("\">").append(escape(module.name)).append("</a>\n");
				module(body, module);
			}
			nav.append("</div>\n");
			body.append("</section>\n");
		});

		String page = template()
			.replace("{{version}}", escape(version))
			.replace("{{stable}}", channel.equals("stable") ? " class=\"active\"" : "")
			.replace("{{nightly}}", channel.equals("nightly") ? " class=\"active\"" : "")
			.replace("{{nav}}", nav)
			.replace("{{body}}", body);
		Files.createDirectories(output.toAbsolutePath().getParent());
		Files.writeString(output, page);
	}

	private static void module(StringBuilder out, Module module) {
		out.append("<article class=\"module\" id=\"").append(module.key).append("\">\n")
			.append("<h3><a href=\"#").append(module.key).append("\">").append(escape(module.name)).append("</a></h3>\n")
			.append(description(module.description)).append('\n');

		if (module.options.isEmpty()) {
			out.append("<p class=\"none\">No settings.</p>\n</article>\n");
			return;
		}

		out.append("<div class=\"settings\">\n");
		for (Option option : module.options) {
			Requires requires = requires(module, option);
			out.append("<div class=\"setting\" data-search=\"").append(escape((option.name + " " + text(option.description)).toLowerCase(Locale.ROOT))).append("\">\n")
				.append("<h4>").append(escape(option.name)).append("</h4>").append(description(option.description));
			if (requires != null) out.append("<p class=\"requires\">").append(requirement(module, requires)).append("</p>");
			out.append("</div>\n");
		}
		out.append("</div>\n</article>\n");
	}

	private static String description(Description description) {
		if (description == null) return "";
		String type = description.type().name().toLowerCase(Locale.ROOT);
		return "<p class=\"" + type + "\">" + escape(description.value()) + "</p>";
	}

	private static String text(Description description) {
		return description == null ? "" : description.value();
	}

	private static Requires requires(Module module, Option option) {
		try {
			return module.getClass().getDeclaredField(option.key).getAnnotation(Requires.class);
		} catch (NoSuchFieldException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String requirement(Module module, Requires requires) {
		Option required = module.options.stream().filter(option -> option.key.equals(requires.value())).findFirst().orElseThrow();
		String name = "<strong>" + escape(required.name) + "</strong>";
		if (requires.is().length == 0) return "Shown when " + name + " is on.";

		StringBuilder values = new StringBuilder();
		for (int i = 0; i < requires.is().length; i++) {
			if (i > 0) values.append(i == requires.is().length - 1 ? " or " : ", ");
			values.append("<em>").append(escape(requires.is()[i])).append("</em>");
		}
		return "Shown when " + name + " is " + values + ".";
	}

	private static String title(Category category) {
		String name = category.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String template() throws IOException {
		try (InputStream in = DocsGenerator.class.getResourceAsStream("/template.html")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
