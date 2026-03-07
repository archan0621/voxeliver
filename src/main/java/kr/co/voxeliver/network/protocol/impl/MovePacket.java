package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class MovePacket implements Packet {
    private int sequence;
    private float x;
    private float y;
    private float z;

    public MovePacket() { }

    public MovePacket(int sequence, float x, float y, float z) {
        this.sequence = sequence;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int getId() {
        return 3;
    }

    @Override
    public void read(ByteBuf buf) {
        sequence = buf.readInt();
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(sequence);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

    public int getSequence() {
        return sequence;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
