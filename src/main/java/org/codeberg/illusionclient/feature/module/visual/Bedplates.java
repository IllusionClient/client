package org.codeberg.illusionclient.feature.module.visual;

import io.github.nevalackin.radbus.Listen;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.EnchantingTableBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowerPotBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LiquidBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.TripwireBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.Blocks;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.ItemRenderer;
import net.minecraft.client.render.platform.GlStateManager;
import net.minecraft.client.render.platform.Lighting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.codeberg.illusionclient.event.BlockChangeEvent;
import org.codeberg.illusionclient.event.ChunkLoadEvent;
import org.codeberg.illusionclient.event.ChunkUnloadEvent;
import org.codeberg.illusionclient.event.Render2DEvent;
import org.codeberg.illusionclient.event.Render3DEvent;
import org.codeberg.illusionclient.event.WorldLoadEvent;
import org.codeberg.illusionclient.feature.config.api.Check;
import org.codeberg.illusionclient.feature.config.api.ColorPicker;
import org.codeberg.illusionclient.feature.config.api.Slider;
import org.codeberg.illusionclient.feature.module.api.Category;
import org.codeberg.illusionclient.feature.module.api.Description;
import org.codeberg.illusionclient.feature.module.api.Module;
import org.codeberg.illusionclient.feature.module.api.ModuleInfo;
import org.codeberg.illusionclient.feature.ui.Draw;
import org.codeberg.illusionclient.mixin.ClientChunkCacheAccessor;
import org.codeberg.illusionclient.util.BlockUtils;

import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.List;

@Description("Shows the blocks defending each bed.")
@ModuleInfo(name = "Bed Plates", category = Category.VISUAL)
public class Bedplates extends Module {
	private static final int FACING_BITS = 3;
	private static final int OCCUPIED_BIT = 4;
	private static final int HEAD_BIT = 8;
	private static final int HEAD_MASK = ~(FACING_BITS | OCCUPIED_BIT);
	private static final int[] FACING_X = {0, -1, 0, 1};
	private static final int[] FACING_Z = {1, 0, -1, 0};
	private static final int[] STEP_X = {1, -1, 0, 0, 0, 0};
	private static final int[] STEP_Y = {0, 0, 0, 0, 1, -1};
	private static final int[] STEP_Z = {0, 0, 1, -1, 0, 0};
	private static final int UP = 4;
	private static final int DOWN = 5;
	private static final byte NEVER = 0, ALWAYS = 1, NOT_BELOW = 2, SIDES = 3;
	private static final int OUTLINE = 0x96000000;
	private static final ItemStack[] NONE = new ItemStack[0];
	private static final int ICON = 16;
	private static final int GAP = 2;
	private static final int PAD = 2;

	private final Minecraft minecraft = Minecraft.getInstance();
	private final Long2ObjectMap<Bed> beds = new Long2ObjectOpenHashMap<>();
	private final List<Bed> visible = new ObjectArrayList<>();
	private int scannedHeight;
	private boolean scannedSkip;

	@Description("How many blocks above the bed to list.")
	@Slider(value = "Height", min = 1.0F, max = 8.0F)
	public float height = 4.0F;

	@Description("Leaves out blocks that aren't full cubes, like slabs, stairs and ladders.")
	@Check("Skip Non-Full Blocks")
	public boolean skipNonFull = false;

	@Description("How round the plate corners are.")
	@Slider(value = "Corner Radius", min = 0.0F, max = 8.0F)
	public float cornerRadius = 3.0F;

	@Description("The plate border when a bed face is exposed.")
	@ColorPicker("Exposed Color")
	public Color exposedColor = new Color(0x80EF7E);

	@Override
	public void onEnable() {
		beds.clear();
		if (minecraft.world == null) return;
		for (WorldChunk chunk : ((ClientChunkCacheAccessor) minecraft.world.getChunkSource()).illusion$chunks()) scan(chunk);
	}

	@Override
	public void onDisable() {
		beds.clear();
		visible.clear();
	}

	@Listen
	public void onWorldLoad(WorldLoadEvent event) {
		beds.clear();
		visible.clear();
	}

	@Listen
	public void onChunkLoad(ChunkLoadEvent event) {
		WorldChunk chunk = event.chunk();
		beds.values().removeIf(bed -> bed.x >> 4 == chunk.chunkX && bed.z >> 4 == chunk.chunkZ);
		scan(chunk);
		for (Bed bed : beds.values()) {
			if (Math.abs((bed.x >> 4) - chunk.chunkX) <= 1 && Math.abs((bed.z >> 4) - chunk.chunkZ) <= 1) bed.dirty = true;
		}
	}

	@Listen
	public void onChunkUnload(ChunkUnloadEvent event) {
		beds.values().removeIf(bed -> bed.x >> 4 == event.chunkX() && bed.z >> 4 == event.chunkZ());
	}

	@Listen
	public void onBlockChange(BlockChangeEvent event) {
		BlockPos pos = event.pos();
		if (isHead(BlockUtils.state(event.oldState()))) beds.remove(pos.toLong());
		int state = BlockUtils.state(event.newState());
		if (isHead(state)) add(pos.getX(), pos.getY(), pos.getZ(), state);

		for (Bed bed : beds.values()) {
			if (pos.getY() < bed.y - 1 || pos.getY() > bed.y + scannedHeight) continue;
			if (pos.getX() < Math.min(bed.x, bed.footX) - 1 || pos.getX() > Math.max(bed.x, bed.footX) + 1) continue;
			if (pos.getZ() < Math.min(bed.z, bed.footZ) - 1 || pos.getZ() > Math.max(bed.z, bed.footZ) + 1) continue;
			bed.dirty = true;
		}
	}

	@Listen
	public void onRender3D(Render3DEvent event) {
		visible.clear();
		if (beds.isEmpty() || minecraft.world == null) return;

		int limit = Math.round(height);
		if (limit != scannedHeight || skipNonFull != scannedSkip) {
			scannedHeight = limit;
			scannedSkip = skipNonFull;
			for (Bed bed : beds.values()) bed.dirty = true;
		}

		EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
		float[] clip = Frustum.getInstance().clipMatrix;
		for (Bed bed : beds.values()) {
			if (bed.dirty) {
				cover(minecraft.world, bed, limit);
				bed.dirty = false;
			}
			if (bed.blocks.length == 0) continue;

			float x = (float) ((bed.x + bed.footX) / 2.0D + 0.5D - dispatcher.cameraX);
			float y = (float) (bed.y + 1.0D - dispatcher.cameraY);
			float z = (float) ((bed.z + bed.footZ) / 2.0D + 0.5D - dispatcher.cameraZ);

			float w = clip[3] * x + clip[7] * y + clip[11] * z + clip[15];
			if (w <= 0.0F) continue;
			float depth = (clip[2] * x + clip[6] * y + clip[10] * z + clip[14]) / w;
			if (depth < -1.0F || depth >= 1.0F) continue;

			bed.screenX = ((clip[0] * x + clip[4] * y + clip[8] * z + clip[12]) / w + 1.0F) * minecraft.width / 2.0F;
			bed.screenY = (1.0F - (clip[1] * x + clip[5] * y + clip[9] * z + clip[13]) / w) * minecraft.height / 2.0F;
			visible.add(bed);
		}
	}

	@Listen
	public void onRender2D(Render2DEvent.Pre event) {
		if (visible.isEmpty()) return;

		int scale = event.window().getScale();
		int plateHeight = ICON + PAD * 2;
		for (Bed bed : visible) {
			int width = bed.blocks.length * (ICON + GAP) - GAP + PAD * 2;
			int x = bed.plateX = Math.round(bed.screenX / scale) - width / 2;
			int y = bed.plateY = Math.round(bed.screenY / scale) - plateHeight;
			int border = bed.exposed ? exposedColor.getRGB() : OUTLINE;

			Draw.rounded(x, y, width, plateHeight, cornerRadius, Draw.HUD_BACKGROUND);
			Draw.roundedOutline(x, y, width, plateHeight, cornerRadius, 1.0F, border);
		}
		Draw.flush();

		ItemRenderer items = minecraft.getItemRenderer();
		GlStateManager.enableRescaleNormal();
		Lighting.turnOnGui();
		for (Bed bed : visible) {
			int x = bed.plateX + PAD;
			for (ItemStack stack : bed.blocks) {
				items.renderGuiItem(stack, x, bed.plateY + PAD);
				items.renderGuiItemDecoration(minecraft.textRenderer, stack, x, bed.plateY + PAD);
				x += ICON + GAP;
			}
		}
		Lighting.turnOff();
		GlStateManager.disableRescaleNormal();
		GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
		GlStateManager.enableBlend();
	}

	private static boolean isHead(int state) {
		return (state & HEAD_MASK) == Lookup.BED_HEAD;
	}

	private void scan(WorldChunk chunk) {
		BlockUtils.scan(chunk, HEAD_MASK, Lookup.BED_HEAD, this::add);
	}

	private void add(int x, int y, int z, int state) {
		beds.put(new BlockPos(x, y, z).toLong(), new Bed(x, y, z, BlockUtils.meta(state) & FACING_BITS));
	}

	private void cover(World world, Bed bed, int limit) {
		int columnX = bed.footX, columnZ = bed.footZ;
		if (BlockUtils.state(world, columnX, bed.y + 1, columnZ) == 0) {
			columnX = bed.x;
			columnZ = bed.z;
			bed.exposed = true;
		} else {
			bed.exposed = exposed(world, bed.footX, bed.y, bed.footZ) || exposed(world, bed.x, bed.y, bed.z);
		}

		List<ItemStack> stacks = new ObjectArrayList<>();
		for (int y = bed.y + 1; y <= Math.min(bed.y + limit, 255); y++) {
			int state = BlockUtils.state(world, columnX, y, columnZ);
			int id = BlockUtils.id(state);
			if (id == 0) break;
			if (id == Lookup.FLOWING_WATER) id = Lookup.WATER;
			else if (id == Lookup.FLOWING_LAVA) id = Lookup.LAVA;
			if (skipNonFull && !Block.byId(id).isCube()) continue;

			ItemStack stack = icon(id, state);
			if (stack == null) continue;
			if (!stacks.isEmpty() && stacks.getFirst().matchesItem(stack)) stacks.getFirst().size++;
			else stacks.addFirst(stack);
		}
		bed.blocks = stacks.toArray(NONE);
	}

	private static boolean exposed(World world, int x, int y, int z) {
		for (int face = 0; face < 6; face++) {
			int id = BlockUtils.id(BlockUtils.state(world, x + STEP_X[face], y + STEP_Y[face], z + STEP_Z[face]));
			switch (Lookup.EXPOSURE[id]) {
				case ALWAYS -> {
					return true;
				}
				case NOT_BELOW -> {
					if (face != DOWN) return true;
				}
				case SIDES -> {
					if (face < UP) return true;
				}
				default -> {
				}
			}
		}
		return false;
	}

	private static byte exposure(Block block) {
		if (block == Blocks.AIR || block == Blocks.IRON_BARS) return ALWAYS;
		if (block == Blocks.END_PORTAL_FRAME) return SIDES;
		if (block instanceof SlabBlock slab) return slab.isDouble() ? NEVER : NOT_BELOW;
		return block instanceof LiquidBlock || block instanceof FireBlock || block instanceof LadderBlock
			|| block instanceof EnderChestBlock || block instanceof EnchantingTableBlock || block instanceof SkullBlock
			|| block instanceof AnvilBlock || block instanceof TripwireBlock || block instanceof VineBlock
			|| block instanceof FlowerPotBlock || block instanceof ChestBlock || block instanceof DoorBlock
			|| block instanceof TrapdoorBlock || block instanceof FenceBlock || block instanceof FenceGateBlock
			|| block instanceof WallBlock || block instanceof StairsBlock ? ALWAYS : NEVER;
	}

	private static ItemStack icon(int id, int state) {
		if (id == Lookup.WATER) return new ItemStack(Items.WATER_BUCKET);
		if (id == Lookup.LAVA) return new ItemStack(Items.LAVA_BUCKET);

		Item item = Item.byBlock(Block.byId(id));
		if (item == null) return null;
		BlockState blockState = BlockUtils.state(state);
		return new ItemStack(item, 1, blockState == null ? 0 : blockState.getBlock().getDropItemMetadata(blockState));
	}

	private static final class Bed {
		final int x, y, z;
		final int footX, footZ;
		ItemStack[] blocks = NONE;
		boolean exposed;
		boolean dirty = true;
		float screenX, screenY;
		int plateX, plateY;

		Bed(int x, int y, int z, int facing) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.footX = x - FACING_X[facing];
			this.footZ = z - FACING_Z[facing];
		}
	}

	private static final class Lookup {
		static final byte[] EXPOSURE = new byte[4096];
		static final int BED_HEAD = BlockUtils.state(Block.getId(Blocks.BED), HEAD_BIT);
		static final int FLOWING_WATER = Block.getId(Blocks.FLOWING_WATER);
		static final int WATER = Block.getId(Blocks.WATER);
		static final int FLOWING_LAVA = Block.getId(Blocks.FLOWING_LAVA);
		static final int LAVA = Block.getId(Blocks.LAVA);

		static {
			for (int id = 0; id < EXPOSURE.length; id++) EXPOSURE[id] = exposure(Block.byId(id));
		}
	}
}
