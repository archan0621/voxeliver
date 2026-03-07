package kr.co.voxeliver.network.protocol;

import io.netty.buffer.ByteBuf;

public interface Packet {

    int getId();

    void read(ByteBuf buf);

    void write(ByteBuf buf);
}
