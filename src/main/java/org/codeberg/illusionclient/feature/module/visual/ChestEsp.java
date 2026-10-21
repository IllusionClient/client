package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.config.api.Combo;
import org.codeberg.illusionclient.feature.config.api.Requires;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.render.EspRenderer;

import java.awt.*;

@Description("Draws boxes around chests.")
@ModuleInfo(name = "Chest ESP", category = Category.VISUAL)
public class ChestEsp extends Module {
	private static final double INSET = 1.0D / 16.0D;
	private static final double HEIGHT = 14.0D / 16.0D;

	private final EspRenderer<BlockEntity> renderer = new EspRenderer<>();

	@Description("2D draws flat boxes on screen, 3D draws boxes in the world.")
	@Combo(value = "Mode", options = {"2D", "3D"})
	public String mode = "2D";

	@Description("The color of chest boxes.")
	@ColorPicker("Chest Color")
	public Color chestColor = new Color(0xFFB74D);

	@Description("Also draws ender chests.")
	@Check("Ender Chests")
	public boolean enderChests = true;

	@Description("The color of ender chest boxes.")
	@Requires("enderChests")
	@ColorPicker("Ender Color")
	public Color enderColor = new Color(0xB388FF);

	@Override
	public void onDisable() {
		renderer.clear();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		renderer.clear();
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		renderer.clear();

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.world == null) return;

		renderer.begin();
		boolean flat = mode.equals("2D");
		for (BlockEntity blockEntity : minecraft.world.blockEntities) {
			Box box = box(blockEntity);
			if (box == null) continue;

			if (flat) renderer.add(blockEntity, box);
			else renderer.box3D(box, color(blockEntity));
		}
		renderer.end3D();
	}

	@Listen
	public void onRender2D(Render2DEvent.Pre event) {
		renderer.render2D(event, (blockEntity, left, top, right, bottom) -> EspRenderer.box(left, top, right, bottom, color(blockEntity)));
	}

	private Box box(BlockEntity blockEntity) {
		BlockPos pos = blockEntity.getPos();
		int width = 1, depth = 1;
		if (blockEntity instanceof ChestBlockEntity chest) {
			chest.updateShape();
			if (chest.westNeighbor != null || chest.northNeighbor != null) return null;
			if (chest.eastNeighbor != null) width = 2;
			if (chest.southNeighbor != null) depth = 2;
		} else if (!(enderChests && blockEntity instanceof EnderChestBlockEntity)) {
			return null;
		}

		return new Box(
			pos.getX() + INSET, pos.getY(), pos.getZ() + INSET,
			pos.getX() + width - INSET, pos.getY() + HEIGHT, pos.getZ() + depth - INSET
		);
	}

	private int color(BlockEntity blockEntity) {
		return (blockEntity instanceof EnderChestBlockEntity ? enderColor : chestColor).getRGB();
	}
}
