package kr.co.voxeliver.network.protocol.impl;

import com.badlogic.gdx.math.Vector3;
import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class LoginAcceptedPacket implements Packet {
    private int playerId;
    private float spawnX;
    private float spawnY;
    private float spawnZ;

    public LoginAcceptedPacket() {
    }

    public LoginAcceptedPacket(int playerId, Vector3 spawnPosition) {
        this.playerId = playerId;
        this.spawnX = spawnPosition.x;
        this.spawnY = spawnPosition.y;
        this.spawnZ = spawnPosition.z;
    }

    @Override
    public int getId() {
        return 5;
    }

    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
        spawnX = buf.readFloat();
        spawnY = buf.readFloat();
        spawnZ = buf.readFloat();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeFloat(spawnX);
        buf.writeFloat(spawnY);
        buf.writeFloat(spawnZ);
    }

    public int getPlayerId() {
        return playerId;
    }

    public Vector3 getSpawnPosition() {
        return new Vector3(spawnX, spawnY, spawnZ);
    }
}
