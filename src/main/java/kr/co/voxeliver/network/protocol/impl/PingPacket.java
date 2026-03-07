package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class PingPacket implements Packet {
    @Override
    public int getId() {
        return 1;
    }

    @Override
    public void read(ByteBuf buf) {

    }

    @Override
    public void write(ByteBuf buf) {

    }

}
