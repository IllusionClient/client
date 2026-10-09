package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.TextRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.ui.Draw;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@ModuleInfo(name = "Mod Overlay", category = Category.VISUAL)
@Description("Lists the enabled modules in the corner of the screen.")
public class ModOverlay extends Module {
	private final List<Row> rows = new ObjectArrayList<>();
	private Row[] all;
	@Slider(value = "Text Size", min = 0.5F, max = 3.0F)
	public float textSize = 1.0F;
	@Slider(value = "Padding X", min = 0.0F, max = 10.0F)
	public float paddingX = 3.0F;
	@Slider(value = "Padding Y", min = 0.0F, max = 10.0F)
	public float paddingY = 2.0F;
	@Slider(value = "Rounding", min = 0.0F, max = 10.0F)
	public float rounding = 3.0F;
	@ColorPicker("Text Color")
	public Color textColor = new Color(46, 204, 113);
	@Check("Show Details")
	public boolean showDetails = true;
	@Requires("showDetails")
	@ColorPicker("Details Color")
	public Color detailsColor = new Color(Draw.MUTED);
	@Check("Lowercase")
	public boolean lowercase = true;
	@Check("Only Bound Modules")
	public boolean onlyBoundModules = false;

	@Listen
	public void onRender2D(Render2DEvent event) {
		TextRenderer renderer = Minecraft.getInstance().textRenderer;
		int gap = renderer.getWidth(' ');

		if (all == null) {
			List<Module> modules = IllusionClient.MODULES.modules;
			all = new Row[modules.size() - 1];
			int count = 0;
			for (Module module : modules) {
				if (module != this) all[count++] = new Row(module);
			}
		}

		rows.clear();
		for (Row row : all) {
			Module module = row.module;
			if (!module.isEnabled()) continue;
			if (onlyBoundModules && module.getBind() == 0) continue;

			row.update(lowercase, showDetails ? module.details() : null);
			row.nameWidth = renderer.getWidth(row.name);
			float textWidth = row.nameWidth + (row.details == null ? 0 : gap + renderer.getWidth(row.details));
			row.width = Math.round(textWidth * textSize + paddingX * 2.0F);
			rows.add(row);
		}
		if (rows.isEmpty()) return;
		rows.sort((a, b) -> b.width - a.width);

		int right = event.window().getWidth();
		int height = Math.round(renderer.fontHeight * textSize + paddingY * 2.0F);

		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			float radius = i + 1 < rows.size() ? Math.min(rounding, row.width - rows.get(i + 1).width) : rounding;
			int overhang = (int) Math.ceil(radius);
			Draw.rounded(right - row.width, i * height, row.width + overhang, height, 0.0F, radius, Draw.BACKGROUND);
		}
		Draw.flush();

		GlStateManager.pushMatrix();
		GlStateManager.scalef(textSize, textSize, 1.0F);
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			float x = (right - row.width + paddingX) / textSize;
			float y = (i * height + paddingY) / textSize + 0.5F;

			Draw.text(row.name, x, y, textColor.getRGB());
			if (row.details != null) Draw.text(row.details, x + row.nameWidth + gap, y, detailsColor.getRGB());
		}
		Draw.flush();
		GlStateManager.popMatrix();

		GlStateManager.enableBlend();
	}

	private static final class Row {
		final Module module;
		String name;
		String details;
		int nameWidth;
		int width;
		private String detailsSource;
		private boolean lowercase;

		Row(Module module) {
			this.module = module;
		}

		void update(boolean lowercase, String source) {
			boolean restyle = name == null || lowercase != this.lowercase;
			this.lowercase = lowercase;
			if (restyle) name = style(module.name);
			if (restyle || !Objects.equals(source, detailsSource)) {
				detailsSource = source;
				details = source == null ? null : style(source);
			}
		}

		private String style(String text) {
			return lowercase ? text.toLowerCase(Locale.ROOT) : text;
		}
	}
}
