package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.PacketData;

public class LoginRequestPacket implements Packet {
    private static final int MAX_USERNAME_BYTES = 32;

    private String username;

    public LoginRequestPacket() {
    }

    public LoginRequestPacket(String username) {
        this.username = username;
    }

    @Override
    public int getId() {
        return 4;
    }

    @Override
    public void read(ByteBuf buf) {
        username = PacketData.readString(buf, MAX_USERNAME_BYTES);
    }

    @Override
    public void write(ByteBuf buf) {
        PacketData.writeString(buf, username, MAX_USERNAME_BYTES);
    }

    public String getUsername() {
        return username;
    }
}
