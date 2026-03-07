package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxeliver.network.protocol.Packet;

public class ChunkUnloadPacket implements Packet {
    private int chunkX;
    private int chunkZ;

    public ChunkUnloadPacket() {
    }

    public ChunkUnloadPacket(ChunkCoord coord) {
        this.chunkX = coord.x;
        this.chunkZ = coord.z;
    }

    @Override
    public int getId() {
        return 7;
    }

    @Override
    public void read(ByteBuf buf) {
        chunkX = buf.readInt();
        chunkZ = buf.readInt();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(chunkX);
        buf.writeInt(chunkZ);
    }

    public ChunkCoord getChunkCoord() {
        return new ChunkCoord(chunkX, chunkZ);
    }
}
