package org.codeberg.illusionclient.mixin;

import net.minecraft.client.world.chunk.ClientChunkCache;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientChunkCache.class)
public interface ClientChunkCacheAccessor {
	@Accessor("chunks")
	List<WorldChunk> illusion$chunks();
}
