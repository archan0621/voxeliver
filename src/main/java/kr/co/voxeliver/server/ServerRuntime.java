package kr.co.voxeliver.server;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.physics.AABB;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxelite.world.ChunkManager;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.protocol.impl.BlockUpdatePacket;
import kr.co.voxeliver.network.protocol.impl.BreakBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.ChunkDataPacket;
import kr.co.voxeliver.network.protocol.impl.ChunkUnloadPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import kr.co.voxeliver.network.protocol.impl.PlaceBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerJoinedPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerLeftPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerStatePacket;
import kr.co.voxeliver.server.player.ServerPlayer;
import kr.co.voxeliver.server.world.FlatWorldGenerator;
import kr.co.voxeliver.server.world.RadiusChunkLoadPolicy;

public class ServerRuntime {
    private final ServerConfig config;
    private final Map<Integer, ServerPlayer> playersById = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final float tickDelta;

    private volatile VoxeliteEngine engine;
    private volatile Vector3 spawnPosition = new Vector3();
    private ScheduledExecutorService tickExecutor;

    public ServerRuntime(ServerConfig config) {
        this.config = config;
        this.tickDelta = 1f / config.tickRate;
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }

        engine = VoxeliteEngine.builder()
            .playerStart(config.spawnPosition.x, config.spawnPosition.y, config.spawnPosition.z)
            .defaultGroundBlockType(config.defaultGroundBlockType)
            .chunkGenerator(new FlatWorldGenerator(config.flatWorldFloorY, config.flatWorldSurfaceY))
            .chunkLoadPolicy(new RadiusChunkLoadPolicy(
                config.visibleChunkRadius,
                config.keepLoadedChunkRadius,
                config.maxLoadedChunks
            ))
            .initialChunkRadius(config.initialChunkRadius)
            .chunkPreloadRadius(config.chunkPreloadRadius)
            .worldSavePath(config.worldPath)
            .build();
        engine.initialize();
        spawnPosition = new Vector3(engine.getPlayer().getPosition());

        tickExecutor = Executors.newSingleThreadScheduledExecutor(new TickThreadFactory());
        running.set(true);
        long tickPeriodMillis = Math.max(1L, Math.round(1000f / config.tickRate));
        tickExecutor.scheduleAtFixedRate(this::runTickSafely, tickPeriodMillis, tickPeriodMillis, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (!running.get()) {
            return;
        }

        running.set(false);
        if (tickExecutor != null) {
            tickExecutor.shutdownNow();
            tickExecutor = null;
        }

        playersById.clear();

        if (engine != null) {
            engine.dispose();
            engine = null;
        }
    }

    public ServerPlayer login(PlayerSession session, String username) {
        ensureRunning();
        String sanitizedUsername = sanitizeUsername(username);
        ServerPlayer existing = playersById.get(session.getPlayerId());
        if (existing != null) {
            existing.touch();
            return existing;
        }

        ServerPlayer created = new ServerPlayer(session, sanitizedUsername, spawnPosition, engine.getWorld());
        playersById.put(created.getPlayerId(), created);
        return created;
    }

    public void disconnect(PlayerSession session) {
        if (session == null) {
            return;
        }
        ServerPlayer removed = playersById.remove(session.getPlayerId());
        if (removed != null) {
            broadcastPlayerLeft(removed);
        }
    }

    public void touch(PlayerSession session) {
        ServerPlayer player = getPlayer(session);
        if (player != null) {
            player.touch();
        }
    }

    public ServerPlayer getPlayer(PlayerSession session) {
        if (session == null) {
            return null;
        }
        return playersById.get(session.getPlayerId());
    }

    public Collection<ServerPlayer> getPlayers() {
        return new ArrayList<>(playersById.values());
    }

    public Vector3 getSpawnPosition() {
        return new Vector3(spawnPosition);
    }

    public VoxeliteEngine getEngine() {
        return engine;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void initializePlayerSession(ServerPlayer player) {
        if (player == null) {
            return;
        }

        for (ServerPlayer other : playersById.values()) {
            if (other.getPlayerId() == player.getPlayerId()) {
                continue;
            }
            player.getSession().send(new PlayerJoinedPacket(
                other.getPlayerId(),
                other.getUsername(),
                other.getPlayer().getPosition()
            ));
        }

        broadcastPlayerJoined(player);
        syncChunksForPlayer(player);
    }

    public boolean handleMove(PlayerSession session, MovePacket movePacket) {
        ServerPlayer player = getPlayer(session);
        if (player == null || movePacket == null) {
            return false;
        }

        player.touch();
        if (movePacket.getSequence() <= player.getLastProcessedMoveSequence()) {
            return false;
        }

        Vector3 requestedPosition = new Vector3(movePacket.getX(), movePacket.getY(), movePacket.getZ());
        if (!isFinite(requestedPosition)) {
            player.setLastProcessedMoveSequence(movePacket.getSequence());
            sendAuthoritativePlayerState(player);
            return false;
        }

        float moveDistance = requestedPosition.dst(player.getPlayer().getPosition());
        if (moveDistance > config.maxMoveDistancePerRequest) {
            player.setLastProcessedMoveSequence(movePacket.getSequence());
            sendAuthoritativePlayerState(player);
            return false;
        }

        player.getPlayer().setPosition(requestedPosition);
        player.setLastProcessedMoveSequence(movePacket.getSequence());
        return true;
    }

    public boolean handleBreakBlock(PlayerSession session, BreakBlockRequestPacket packet) {
        ServerPlayer player = getPlayer(session);
        if (player == null || packet == null) {
            return false;
        }

        player.touch();
        Vector3 blockPosition = packet.getPosition();
        if (!canInteractWithBlock(player, blockPosition)) {
            sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }
        if (!engine.getWorld().hasBlock(blockPosition)) {
            sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        if (!engine.removeBlock(blockPosition)) {
            sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        invalidateAllPhysicsCaches();
        broadcastBlockUpdate(new BlockUpdatePacket(blockPosition, -1));
        return true;
    }

    public boolean handlePlaceBlock(PlayerSession session, PlaceBlockRequestPacket packet) {
        ServerPlayer player = getPlayer(session);
        if (player == null || packet == null) {
            return false;
        }

        player.touch();
        Vector3 blockPosition = packet.getPosition();
        if (!canInteractWithBlock(player, blockPosition) || packet.getBlockType() < 0) {
            sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }
        if (engine.getWorld().hasBlock(blockPosition) || wouldCollideWithPlayer(player, blockPosition)) {
            sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        engine.addBlock(blockPosition, packet.getBlockType());
        invalidateAllPhysicsCaches();
        broadcastBlockUpdate(new BlockUpdatePacket(blockPosition, packet.getBlockType()));
        return true;
    }

    void tick() {
        ensureRunning();
        updateWorldStreaming();
        pruneTimedOutPlayers();
        for (ServerPlayer player : playersById.values()) {
            player.tick(tickDelta);
        }
        for (ServerPlayer player : playersById.values()) {
            syncChunksForPlayer(player);
        }
        broadcastPlayerStates();
    }

    private void runTickSafely() {
        try {
            if (running.get()) {
                tick();
            }
        } catch (Exception e) {
            System.err.println("[ServerRuntime] Tick failure: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void pruneTimedOutPlayers() {
        long now = System.currentTimeMillis();
        for (ServerPlayer player : playersById.values()) {
            if (now - player.getLastHeartbeatAt() <= config.keepAliveTimeoutMillis) {
                continue;
            }

            playersById.remove(player.getPlayerId());
            player.getSession().getChannel().close();
        }
    }

    private void updateWorldStreaming() {
        List<Vector3> playerPositions = new ArrayList<>();
        for (ServerPlayer player : playersById.values()) {
            playerPositions.add(new Vector3(player.getPlayer().getPosition()));
        }

        if (playerPositions.isEmpty()) {
            playerPositions.add(new Vector3(spawnPosition));
        }

        engine.getWorld().updateChunks(playerPositions);
        engine.getWorld().processPendingChunks();
    }

    private void syncChunksForPlayer(ServerPlayer player) {
        ChunkManager chunkManager = engine.getWorld().getChunkManager();
        if (chunkManager == null) {
            return;
        }

        Set<ChunkCoord> visibleChunks = getVisibleChunkCoords(player.getPlayer().getPosition());

        for (ChunkCoord coord : player.getLoadedChunksSnapshot()) {
            if (visibleChunks.contains(coord)) {
                continue;
            }
            player.forgetChunk(coord);
            player.getSession().send(new ChunkUnloadPacket(coord));
        }

        for (ChunkCoord coord : visibleChunks) {
            Chunk chunk = chunkManager.getChunk(coord);
            if (chunk == null || !chunk.isGenerated()) {
                continue;
            }
            if (player.rememberChunk(coord)) {
                player.getSession().send(new ChunkDataPacket(chunk));
            }
        }
    }

    private Set<ChunkCoord> getVisibleChunkCoords(Vector3 position) {
        Set<ChunkCoord> coords = new HashSet<>();
        ChunkCoord center = ChunkCoord.fromWorldPos(position.x, position.z, Chunk.CHUNK_SIZE);
        for (int dx = -config.visibleChunkRadius; dx <= config.visibleChunkRadius; dx++) {
            for (int dz = -config.visibleChunkRadius; dz <= config.visibleChunkRadius; dz++) {
                coords.add(new ChunkCoord(center.x + dx, center.z + dz));
            }
        }
        return coords;
    }

    private void broadcastPlayerStates() {
        List<ServerPlayer> snapshot = new ArrayList<>(playersById.values());
        for (ServerPlayer recipient : snapshot) {
            for (ServerPlayer player : snapshot) {
                recipient.getSession().send(new PlayerStatePacket(
                    player.getPlayerId(),
                    player.getLastProcessedMoveSequence(),
                    player.getPlayer().getPosition()
                ));
            }
        }
    }

    private void broadcastPlayerJoined(ServerPlayer joinedPlayer) {
        for (ServerPlayer recipient : playersById.values()) {
            if (recipient.getPlayerId() == joinedPlayer.getPlayerId()) {
                continue;
            }

            recipient.getSession().send(new PlayerJoinedPacket(
                joinedPlayer.getPlayerId(),
                joinedPlayer.getUsername(),
                joinedPlayer.getPlayer().getPosition()
            ));
        }
    }

    private void broadcastPlayerLeft(ServerPlayer removedPlayer) {
        PlayerLeftPacket packet = new PlayerLeftPacket(removedPlayer.getPlayerId());
        for (ServerPlayer recipient : playersById.values()) {
            recipient.getSession().send(packet);
        }
    }

    private void broadcastBlockUpdate(BlockUpdatePacket packet) {
        for (ServerPlayer recipient : playersById.values()) {
            recipient.getSession().send(packet);
        }
    }

    private void sendAuthoritativePlayerState(ServerPlayer player) {
        player.getSession().send(new PlayerStatePacket(
            player.getPlayerId(),
            player.getLastProcessedMoveSequence(),
            player.getPlayer().getPosition()
        ));
    }

    private void sendAuthoritativeBlockState(ServerPlayer player, Vector3 blockPosition) {
        int blockType = engine.getWorld().getBlockType(blockPosition);
        player.getSession().send(new BlockUpdatePacket(blockPosition, blockType));
    }

    private boolean canInteractWithBlock(ServerPlayer player, Vector3 blockPosition) {
        return blockPosition != null
            && isFinite(blockPosition)
            && player.getPlayer().getEyePosition().dst(blockPosition) <= config.interactionRange;
    }

    private boolean wouldCollideWithPlayer(ServerPlayer player, Vector3 blockPosition) {
        AABB blockAABB = new AABB(new Vector3(
            MathUtils.floor(blockPosition.x),
            MathUtils.floor(blockPosition.y),
            MathUtils.floor(blockPosition.z)
        ), 0.5f);
        return player.getPlayer().getAABB().intersects(blockAABB);
    }

    private void invalidateAllPhysicsCaches() {
        if (engine.getPhysics() != null) {
            engine.getPhysics().invalidateCache();
        }

        for (ServerPlayer player : playersById.values()) {
            player.getPhysics().invalidateCache();
        }
    }

    private boolean isFinite(Vector3 position) {
        return position != null
            && Float.isFinite(position.x)
            && Float.isFinite(position.y)
            && Float.isFinite(position.z);
    }

    private void ensureRunning() {
        if (!running.get() || engine == null) {
            throw new IllegalStateException("Server runtime is not running");
        }
    }

    private String sanitizeUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("username is required");
        }

        String sanitized = username.trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("username is required");
        }

        return sanitized;
    }

    private static final class TickThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "voxeliver-server-tick");
            thread.setDaemon(true);
            return thread;
        }
    }
}
