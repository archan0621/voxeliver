package kr.co.voxeliver.server.world;

import kr.co.voxelite.world.IChunkLoadPolicy;

public class RadiusChunkLoadPolicy implements IChunkLoadPolicy {
    private final int visibleRadius;
    private final int keepLoadedRadius;
    private final int maxLoadedChunks;

    public RadiusChunkLoadPolicy(int visibleRadius, int keepLoadedRadius, int maxLoadedChunks) {
        this.visibleRadius = visibleRadius;
        this.keepLoadedRadius = keepLoadedRadius;
        this.maxLoadedChunks = maxLoadedChunks;
    }

    @Override
    public boolean shouldLoadToMemory(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        return isWithinRadius(chunkX, chunkZ, playerChunkX, playerChunkZ, visibleRadius);
    }

    @Override
    public boolean shouldKeepLoaded(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        return isWithinRadius(chunkX, chunkZ, playerChunkX, playerChunkZ, keepLoadedRadius);
    }

    @Override
    public boolean shouldPregenerate(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ) {
        return false;
    }

    @Override
    public int getMaxLoadedChunks() {
        return maxLoadedChunks;
    }

    private boolean isWithinRadius(int chunkX, int chunkZ, int playerChunkX, int playerChunkZ, int radius) {
        int dx = chunkX - playerChunkX;
        int dz = chunkZ - playerChunkZ;
        return dx * dx + dz * dz <= radius * radius;
    }
}
