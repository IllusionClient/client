package org.codeberg.illusionclient.event;

import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;

public final class BlockChangeEvent implements Event {
	private static final BlockChangeEvent INSTANCE = new BlockChangeEvent();
	private BlockPos pos;
	private BlockState oldState;
	private BlockState newState;

	private BlockChangeEvent() {
	}

	public static BlockChangeEvent of(BlockPos pos, BlockState oldState, BlockState newState) {
		INSTANCE.pos = pos;
		INSTANCE.oldState = oldState;
		INSTANCE.newState = newState;
		return INSTANCE;
	}

	public BlockPos pos() {
		return pos;
	}

	public BlockState oldState() {
		return oldState;
	}

	public BlockState newState() {
		return newState;
	}
}
