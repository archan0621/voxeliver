package kr.co.voxeliver.network.protocol.impl;

import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class PlayerLeftPacket implements Packet {
    private int playerId;

    public PlayerLeftPacket() {
    }

    public PlayerLeftPacket(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public int getId() {
        return 10;
    }

    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
    }

    public int getPlayerId() {
        return playerId;
    }
}
