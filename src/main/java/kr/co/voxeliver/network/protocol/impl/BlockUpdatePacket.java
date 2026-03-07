package kr.co.voxeliver.network.protocol.impl;

import com.badlogic.gdx.math.Vector3;
import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class BlockUpdatePacket implements Packet {
    private int x;
    private int y;
    private int z;
    private int blockType;

    public BlockUpdatePacket() {
    }

    public BlockUpdatePacket(int x, int y, int z, int blockType) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockType = blockType;
    }

    public BlockUpdatePacket(Vector3 position, int blockType) {
        this((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z), blockType);
    }

    @Override
    public int getId() {
        return 11;
    }

    @Override
    public void read(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
        blockType = buf.readInt();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(blockType);
    }

    public Vector3 getPosition() {
        return new Vector3(x, y, z);
    }

    public int getBlockType() {
        return blockType;
    }

    public boolean isRemoval() {
        return blockType < 0;
    }
}
