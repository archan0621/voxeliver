package kr.co.voxeliver.network.protocol.impl;

import com.badlogic.gdx.math.Vector3;
import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.PacketData;

public class PlayerJoinedPacket implements Packet {
    private static final int MAX_USERNAME_BYTES = 32;

    private int playerId;
    private String username;
    private float x;
    private float y;
    private float z;

    public PlayerJoinedPacket() {
    }

    public PlayerJoinedPacket(int playerId, String username, Vector3 position) {
        this.playerId = playerId;
        this.username = username;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    @Override
    public int getId() {
        return 8;
    }

    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
        username = PacketData.readString(buf, MAX_USERNAME_BYTES);
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
        PacketData.writeString(buf, username, MAX_USERNAME_BYTES);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public Vector3 getPosition() {
        return new Vector3(x, y, z);
    }
}
