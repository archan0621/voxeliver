package kr.co.voxeliver.network.protocol.impl;

import com.badlogic.gdx.math.Vector3;
import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class BreakBlockRequestPacket implements Packet {
    private int x;
    private int y;
    private int z;

    public BreakBlockRequestPacket() {
    }

    public BreakBlockRequestPacket(Vector3 position) {
        this((int) Math.floor(position.x), (int) Math.floor(position.y), (int) Math.floor(position.z));
    }

    public BreakBlockRequestPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public int getId() {
        return 12;
    }

    @Override
    public void read(ByteBuf buf) {
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    public Vector3 getPosition() {
        return new Vector3(x, y, z);
    }
}
