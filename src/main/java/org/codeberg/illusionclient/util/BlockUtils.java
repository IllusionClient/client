package org.codeberg.illusionclient.util;

import net.minecraft.block.Block;
import net.minecraft.block.state.BlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public final class BlockUtils {
	@FunctionalInterface
	public interface Visitor {
		void visit(int x, int y, int z, int state);
	}

	public static int state(int id, int meta) {
		return id << 4 | meta;
	}

	public static int state(BlockState state) {
		return Block.STATE_REGISTRY.getId(state);
	}

	public static BlockState state(int state) {
		return Block.STATE_REGISTRY.get(state);
	}

	public static int id(int state) {
		return state >> 4;
	}

	public static int meta(int state) {
		return state & 15;
	}

	public static int state(World world, int x, int y, int z) {
		return state(world.getChunkAt(x >> 4, z >> 4), x, y, z);
	}

	public static int state(WorldChunk chunk, int x, int y, int z) {
		if (y < 0 || y > 255) return 0;

		WorldChunkSection section = chunk.getSections()[y >> 4];
		return section == null ? 0 : section.getBlockStates()[(y & 15) << 8 | (z & 15) << 4 | (x & 15)];
	}

	public static void scan(WorldChunk chunk, int mask, int match, Visitor visitor) {
		int baseX = chunk.chunkX << 4;
		int baseZ = chunk.chunkZ << 4;
		for (WorldChunkSection section : chunk.getSections()) {
			if (section == null || section.isEmpty()) continue;

			char[] states = section.getBlockStates();
			for (int i = 0; i < states.length; i++) {
				if ((states[i] & mask) != match) continue;
				visitor.visit(baseX + (i & 15), section.getOffsetY() + (i >> 8), baseZ + (i >> 4 & 15), states[i]);
			}
		}
	}
}
