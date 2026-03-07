package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.PacketData;

public class ChatPacket implements Packet {
    private static final int MAX_MESSAGE_BYTES = 1024;

    private String message;

    public ChatPacket() {
    }

    public ChatPacket(final String message) {
        this.message = message;
    }

    @Override
    public int getId() {
        return 2;
    }

    @Override
    public void read(ByteBuf buf) {
        message = PacketData.readString(buf, MAX_MESSAGE_BYTES);
    }

    @Override
    public void write(ByteBuf buf) {
        PacketData.writeString(buf, message, MAX_MESSAGE_BYTES);
    }

    public String getMessage() {
        return message;
    }
}
