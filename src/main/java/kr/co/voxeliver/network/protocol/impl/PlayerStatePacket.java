package kr.co.voxeliver.network.protocol.impl;

import com.badlogic.gdx.math.Vector3;
import io.netty.buffer.ByteBuf;
import kr.co.voxeliver.network.protocol.Packet;

public class PlayerStatePacket implements Packet {
    private int playerId;
    private int acknowledgedMoveSequence;
    private float x;
    private float y;
    private float z;

    public PlayerStatePacket() {
    }

    public PlayerStatePacket(int playerId, int acknowledgedMoveSequence, Vector3 position) {
        this.playerId = playerId;
        this.acknowledgedMoveSequence = acknowledgedMoveSequence;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
    }

    @Override
    public int getId() {
        return 9;
    }

    @Override
    public void read(ByteBuf buf) {
        playerId = buf.readInt();
        acknowledgedMoveSequence = buf.readInt();
        x = buf.readFloat();
        y = buf.readFloat();
        z = buf.readFloat();
    }

    @Override
    public void write(ByteBuf buf) {
        buf.writeInt(playerId);
        buf.writeInt(acknowledgedMoveSequence);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getAcknowledgedMoveSequence() {
        return acknowledgedMoveSequence;
    }

    public Vector3 getPosition() {
        return new Vector3(x, y, z);
    }
}
