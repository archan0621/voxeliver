package kr.co.voxeliver.network;

import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.session.SessionManager;

public class BroadCast {

    public static void send(Packet packet) {
        send(packet, null);
    }

    public static void send(Packet packet, PlayerSession excluded) {
        for (PlayerSession session : SessionManager.getSessions()) {
            if (session == excluded) {
                continue;
            }
            session.send(packet);
        }
    }
}
