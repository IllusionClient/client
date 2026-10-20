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
import java.util.Base64;
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

		StringBuilder icons = new StringBuilder();
		for (String icon : List.of("logo", "search")) icons.append(icon(icon));
		StringBuilder nav = new StringBuilder();
		StringBuilder body = new StringBuilder();
		for (Map.Entry<Category, List<Module>> entry : byCategory.entrySet()) {
			Category category = entry.getKey();
			List<Module> modules = entry.getValue();
			String title = title(category);
			String id = category.name().toLowerCase(Locale.ROOT);
			icons.append(icon(id));
			nav.append("<div class=\"group\">\n<a class=\"entry category\" href=\"#").append(id).append("\"><span class=\"icon\" style=\"--icon: var(--icon-").append(id).append(")\"></span>")
				.append(title).append("<span class=\"count\">").append(modules.size()).append("</span></a>\n");
			body.append("<section class=\"category\" id=\"").append(id).append("\">\n")
				.append("<h2><span class=\"icon\" style=\"--icon: var(--icon-").append(id).append(")\"></span>").append(title).append("</h2>\n");
			for (Module module : modules) {
				nav.append("<a class=\"entry\" href=\"#").append(module.key).append("\">").append(escape(module.name)).append("</a>\n");
				module(body, module);
			}
			nav.append("</div>\n");
			body.append("</section>\n");
		}
		int count = byCategory.values().stream().mapToInt(List::size).sum();

		String page = template()
			.replace("{{icons}}", icons)
			.replace("{{count}}", String.valueOf(count))
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
			.append("<header class=\"module-head\">\n<div>\n<h3><a href=\"#").append(module.key).append("\">").append(escape(module.name)).append("</a></h3>\n")
			.append(description(module.description)).append("\n</div>\n</header>\n");

		if (module.options.isEmpty()) {
			out.append("</article>\n");
			return;
		}

		out.append("<div class=\"settings\">\n");
		for (Option option : module.options) {
			Requires requires = requires(module, option);
			out.append("<div class=\"setting\" data-search=\"").append(escape((option.name + " " + text(option.description)).toLowerCase(Locale.ROOT))).append("\">\n")
				.append("<h4>").append(escape(option.name)).append("</h4>").append(description(option.description));
			if (requires != null) out.append("<p class=\"requires\">").append(requirement(module, requires)).append("</p>");
			out.append("\n</div>\n");
		}
		out.append("</div>\n</article>\n");
	}

	private static String list(String[] values) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			if (i > 0) out.append(i == values.length - 1 ? " or " : ", ");
			out.append("<em>").append(escape(values[i])).append("</em>");
		}
		return out.toString();
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
		return "Shown when " + name + " is " + list(requires.is()) + ".";
	}

	private static String title(Category category) {
		String name = category.name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	private static String icon(String name) throws IOException {
		try (InputStream in = DocsGenerator.class.getResourceAsStream("/assets/illusion/textures/gui/icons/smooth/" + name + ".png")) {
			return "--icon-" + name + ": url(\"data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes()) + "\");\n";
		}
	}

	private static String template() throws IOException {
		try (InputStream in = DocsGenerator.class.getResourceAsStream("/template.html")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
