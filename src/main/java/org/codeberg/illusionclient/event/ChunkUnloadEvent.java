package org.codeberg.illusionclient.event;

public final class ChunkUnloadEvent implements Event {
	private static final ChunkUnloadEvent INSTANCE = new ChunkUnloadEvent();
	private int chunkX;
	private int chunkZ;

	private ChunkUnloadEvent() {
	}

	public static ChunkUnloadEvent of(int chunkX, int chunkZ) {
		INSTANCE.chunkX = chunkX;
		INSTANCE.chunkZ = chunkZ;
		return INSTANCE;
	}

	public int chunkX() {
		return chunkX;
	}

	public int chunkZ() {
		return chunkZ;
	}
}
