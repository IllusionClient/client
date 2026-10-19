package org.codeberg.illusionclient.mixin;

import net.minecraft.block.state.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.BlockChangeEvent;
import org.codeberg.illusionclient.event.ChunkLoadEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldChunk.class)
public class WorldChunkMixin {
	@Shadow
	@Final
	private World world;

	@Inject(method = "update", at = @At("TAIL"))
	private void illusion$chunkLoad(byte[] bs, int i, boolean bl, CallbackInfo ci) {
		IllusionClient.BUS.publish(ChunkLoadEvent.of((WorldChunk) (Object) this));
	}

	@Inject(method = "setBlockState", at = @At("RETURN"))
	private void illusion$blockChange(BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<BlockState> cir) {
		BlockState old = cir.getReturnValue();
		if (old != null && world.isClient) IllusionClient.BUS.publish(BlockChangeEvent.of(blockPos, old, blockState));
	}
}
