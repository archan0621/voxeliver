package kr.co.voxeliver.server.player;

import com.badlogic.gdx.math.Vector3;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import kr.co.voxelite.entity.Player;
import kr.co.voxelite.physics.PhysicsSystem;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxelite.world.World;
import kr.co.voxeliver.network.session.PlayerSession;

public class ServerPlayer {
    private final int playerId;
    private final String username;
    private final PlayerSession session;
    private final Player player;
    private final PhysicsSystem physics;
    private final Set<ChunkCoord> loadedChunks = ConcurrentHashMap.newKeySet();

    private volatile long lastHeartbeatAt;
    private volatile int lastProcessedMoveSequence;

    public ServerPlayer(PlayerSession session, String username, Vector3 spawnPosition, World world) {
        this.playerId = session.getPlayerId();
        this.username = username;
        this.session = session;
        this.player = new Player(new Vector3(spawnPosition));
        this.physics = new PhysicsSystem(world);
        touch();
    }

    public void tick(float delta) {
        physics.update(player, delta);
    }

    public void touch() {
        lastHeartbeatAt = System.currentTimeMillis();
    }

    public int getPlayerId() {
        return playerId;
    }

    public String getUsername() {
        return username;
    }

    public PlayerSession getSession() {
        return session;
    }

    public Player getPlayer() {
        return player;
    }

    public PhysicsSystem getPhysics() {
        return physics;
    }

    public long getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public boolean rememberChunk(ChunkCoord coord) {
        return loadedChunks.add(coord);
    }

    public boolean forgetChunk(ChunkCoord coord) {
        return loadedChunks.remove(coord);
    }

    public Set<ChunkCoord> getLoadedChunksSnapshot() {
        return new HashSet<>(loadedChunks);
    }

    public int getLastProcessedMoveSequence() {
        return lastProcessedMoveSequence;
    }

    public void setLastProcessedMoveSequence(int lastProcessedMoveSequence) {
        this.lastProcessedMoveSequence = lastProcessedMoveSequence;
    }
}
