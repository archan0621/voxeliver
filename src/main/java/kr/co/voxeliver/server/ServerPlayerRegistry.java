package kr.co.voxeliver.server;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.co.voxelite.world.World;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.server.player.ServerPlayer;

class ServerPlayerRegistry {
    private final Map<Integer, ServerPlayer> playersById = new ConcurrentHashMap<>();

    public ServerPlayer login(PlayerSession session, String username, Vector3 spawnPosition, World world) {
        ServerPlayer existing = playersById.get(session.getPlayerId());
        if (existing != null) {
            existing.touch();
            return existing;
        }

        ServerPlayer created = new ServerPlayer(session, username, spawnPosition, world);
        playersById.put(created.getPlayerId(), created);
        return created;
    }

    public ServerPlayer disconnect(PlayerSession session) {
        if (session == null) {
            return null;
        }
        return playersById.remove(session.getPlayerId());
    }

    public void touch(PlayerSession session) {
        ServerPlayer player = get(session);
        if (player != null) {
            player.touch();
        }
    }

    public ServerPlayer get(PlayerSession session) {
        if (session == null) {
            return null;
        }
        return playersById.get(session.getPlayerId());
    }

    public Collection<ServerPlayer> snapshot() {
        return new ArrayList<>(playersById.values());
    }

    public void clear() {
        playersById.clear();
    }

    public List<ServerPlayer> removeTimedOut(long now, long keepAliveTimeoutMillis) {
        List<ServerPlayer> removedPlayers = new ArrayList<>();
        for (ServerPlayer player : playersById.values()) {
            if (now - player.getLastHeartbeatAt() <= keepAliveTimeoutMillis) {
                continue;
            }

            if (playersById.remove(player.getPlayerId(), player)) {
                removedPlayers.add(player);
            }
        }
        return removedPlayers;
    }
}
