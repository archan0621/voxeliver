package kr.co.voxeliver.network.session;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Set<PlayerSession> sessions = ConcurrentHashMap.newKeySet();

    public static void add(PlayerSession session) {
        sessions.add(session);
    }

    public static void remove(PlayerSession session) {
        sessions.remove(session);
    }

    public static Set<PlayerSession> getSessions() {
        return sessions;
    }

    public static void clear() {
        sessions.clear();
    }
}
