package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import kr.co.voxelite.world.BlockPos;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxeliver.network.protocol.Packet;

public class ChunkDataPacket implements Packet {
    private int chunkX;
    private int chunkZ;
    private List<BlockEntry> blocks = new ArrayList<>();

    public ChunkDataPacket() {
    }

    public ChunkDataPacket(Chunk chunk) {
        this.chunkX = chunk.getCoord().x;
        this.chunkZ = chunk.getCoord().z;
        this.blocks = new ArrayList<>(chunk.getBlocks().size());
        for (Chunk.BlockData block : chunk.getBlocks()) {
            blocks.add(new BlockEntry(block.pos.x(), block.pos.y(), block.pos.z(), block.blockType));
        }
    }

    @Override
    public int getId() {
        return 6;
    }

    @Override
    public void read(ByteBuf buf) {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
        int blockCount = buf.readInt();
        if (blockCount < 0) {
            throw new IllegalArgumentException("Invalid block count: " + blockCount);
        }

        blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            blocks.add(new BlockEntry(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readInt()
            ));
        }
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
        buf.writeInt(blocks.size());
        for (BlockEntry block : blocks) {
            buf.writeInt(block.localX());
            buf.writeInt(block.worldY());
            buf.writeInt(block.localZ());
            buf.writeInt(block.blockType());
        }
    }

    public ChunkCoord getChunkCoord() {
        return new ChunkCoord(chunkX, chunkZ);
    }

    public Chunk toChunk() {
        Chunk chunk = new Chunk(getChunkCoord());
        for (BlockEntry block : blocks) {
            chunk.addBlockLocal(block.localX(), block.worldY(), block.localZ(), block.blockType());
        }
        chunk.markAsGenerated();
        chunk.setModified(false);
        return chunk;
    }

    public List<BlockPos> getBlockPositions() {
        List<BlockPos> positions = new ArrayList<>(blocks.size());
        for (BlockEntry block : blocks) {
            positions.add(new BlockPos(block.localX(), block.worldY(), block.localZ()));
        }
        return positions;
    }

    private record BlockEntry(int localX, int worldY, int localZ, int blockType) {
    }
}
