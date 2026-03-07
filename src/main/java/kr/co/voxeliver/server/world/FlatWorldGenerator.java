package kr.co.voxeliver.server.world;

import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.IChunkGenerator;

public class FlatWorldGenerator implements IChunkGenerator {
    private final int floorY;
    private final int surfaceY;

    public FlatWorldGenerator(int floorY, int surfaceY) {
        this.floorY = floorY;
        this.surfaceY = surfaceY;
    }

    @Override
    public void generateChunk(Chunk chunk, int blockType) {
        for (int localX = 0; localX < Chunk.CHUNK_SIZE; localX++) {
            for (int localZ = 0; localZ < Chunk.CHUNK_SIZE; localZ++) {
                for (int y = floorY; y <= surfaceY; y++) {
                    chunk.addBlockLocal(localX, y, localZ, blockType);
                }
            }
        }
    }
}
