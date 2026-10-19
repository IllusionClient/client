package org.codeberg.illusionclient.mixin;

import net.minecraft.client.world.chunk.ClientChunkCache;
import org.codeberg.illusionclient.IllusionClient;
import org.codeberg.illusionclient.event.ChunkUnloadEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientChunkCache.class)
public class ClientChunkCacheMixin {
	@Inject(method = "unloadChunk", at = @At("HEAD"))
	private void illusion$chunkUnload(int i, int j, CallbackInfo ci) {
		IllusionClient.BUS.publish(ChunkUnloadEvent.of(i, j));
	}
}
