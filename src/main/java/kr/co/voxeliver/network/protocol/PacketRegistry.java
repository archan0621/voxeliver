package kr.co.voxeliver.network.protocol;

import java.util.HashMap;
import java.util.Map;
import kr.co.voxeliver.network.protocol.impl.BlockUpdatePacket;
import kr.co.voxeliver.network.protocol.impl.BreakBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.ChatPacket;
import kr.co.voxeliver.network.protocol.impl.ChunkDataPacket;
import kr.co.voxeliver.network.protocol.impl.ChunkUnloadPacket;
import kr.co.voxeliver.network.protocol.impl.LoginAcceptedPacket;
import kr.co.voxeliver.network.protocol.impl.LoginRequestPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import kr.co.voxeliver.network.protocol.impl.PlaceBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerJoinedPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerLeftPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerStatePacket;
import kr.co.voxeliver.network.protocol.impl.PingPacket;

public class PacketRegistry {

    private static final Map<Integer, Class<? extends Packet>> packets = new HashMap<>();

    static {
        packets.put(1, PingPacket.class);
        packets.put(2, ChatPacket.class);
        packets.put(3, MovePacket.class);
        packets.put(4, LoginRequestPacket.class);
        packets.put(5, LoginAcceptedPacket.class);
        packets.put(6, ChunkDataPacket.class);
        packets.put(7, ChunkUnloadPacket.class);
        packets.put(8, PlayerJoinedPacket.class);
        packets.put(9, PlayerStatePacket.class);
        packets.put(10, PlayerLeftPacket.class);
        packets.put(11, BlockUpdatePacket.class);
        packets.put(12, BreakBlockRequestPacket.class);
        packets.put(13, PlaceBlockRequestPacket.class);
    }

    public static Packet create(int id) {
        Class<? extends Packet> clazz = packets.get(id);

        if (clazz == null) {
            throw new RuntimeException("Unknown packet id " + id);
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create packet for id " + id, e);
        }
    }
}
