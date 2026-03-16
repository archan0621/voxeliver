package kr.co.voxeliver.server;

import com.badlogic.gdx.math.Vector3;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.protocol.impl.BlockUpdatePacket;
import kr.co.voxeliver.network.protocol.impl.BreakBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import kr.co.voxeliver.network.protocol.impl.PlaceBlockRequestPacket;
import kr.co.voxeliver.server.player.ServerPlayer;
import kr.co.voxeliver.server.world.FlatWorldGenerator;
import kr.co.voxeliver.server.world.RadiusChunkLoadPolicy;

public class ServerRuntime {
    private final ServerConfig config;
    private final ServerPlayerRegistry playerRegistry = new ServerPlayerRegistry();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile VoxeliteEngine engine;
    private volatile Vector3 spawnPosition = new Vector3();
    private volatile ServerWorldCoordinator worldCoordinator;
    private ScheduledExecutorService tickExecutor;

    public ServerRuntime(ServerConfig config) {
        this.config = config;
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
        worldCoordinator = new ServerWorldCoordinator(config, engine, playerRegistry);

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

        playerRegistry.clear();
        worldCoordinator = null;

        if (engine != null) {
            engine.dispose();
            engine = null;
        }
    }

    public ServerPlayer login(PlayerSession session, String username) {
        ensureRunning();
        String sanitizedUsername = sanitizeUsername(username);
        return playerRegistry.login(session, sanitizedUsername, spawnPosition, engine.getWorld());
    }

    public void disconnect(PlayerSession session) {
        ServerPlayer removed = playerRegistry.disconnect(session);
        if (removed != null && worldCoordinator != null) {
            worldCoordinator.broadcastPlayerLeft(removed);
        }
    }

    public void touch(PlayerSession session) {
        playerRegistry.touch(session);
    }

    public ServerPlayer getPlayer(PlayerSession session) {
        return playerRegistry.get(session);
    }

    public Collection<ServerPlayer> getPlayers() {
        return playerRegistry.snapshot();
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
        if (worldCoordinator != null) {
            worldCoordinator.initializePlayerSession(player);
        }
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
            worldCoordinator.sendAuthoritativePlayerState(player);
            return false;
        }

        float moveDistance = requestedPosition.dst(player.getPlayer().getPosition());
        if (moveDistance > config.maxMoveDistancePerRequest) {
            player.setLastProcessedMoveSequence(movePacket.getSequence());
            worldCoordinator.sendAuthoritativePlayerState(player);
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
            worldCoordinator.sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }
        if (!engine.getWorld().hasBlock(blockPosition)) {
            worldCoordinator.sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        if (!engine.removeBlock(blockPosition)) {
            worldCoordinator.sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        worldCoordinator.invalidateAllPhysicsCaches();
        worldCoordinator.broadcastBlockUpdate(new BlockUpdatePacket(blockPosition, -1));
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
            worldCoordinator.sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }
        if (engine.getWorld().hasBlock(blockPosition) || wouldCollideWithPlayer(player, blockPosition)) {
            worldCoordinator.sendAuthoritativeBlockState(player, blockPosition);
            return false;
        }

        engine.addBlock(blockPosition, packet.getBlockType());
        worldCoordinator.invalidateAllPhysicsCaches();
        worldCoordinator.broadcastBlockUpdate(new BlockUpdatePacket(blockPosition, packet.getBlockType()));
        return true;
    }

    void tick() {
        ensureRunning();
        worldCoordinator.updateWorldStreaming(spawnPosition);
        pruneTimedOutPlayers();
        // The current multiplayer protocol sends client-predicted absolute positions.
        // Running independent server gravity here fights jump arcs and causes visible snapping.
        worldCoordinator.syncChunksForAllPlayers();
        worldCoordinator.broadcastPlayerStates();
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
        for (ServerPlayer player : playerRegistry.removeTimedOut(now, config.keepAliveTimeoutMillis)) {
            player.getSession().getChannel().close();
        }
    }

    private boolean canInteractWithBlock(ServerPlayer player, Vector3 blockPosition) {
        return blockPosition != null
            && isFinite(blockPosition)
            && player.getPlayer().getEyePosition().dst(blockPosition) <= config.interactionRange;
    }

    private boolean wouldCollideWithPlayer(ServerPlayer player, Vector3 blockPosition) {
        return player.getPlayer().collidesWithBlock(blockPosition);
    }

    private boolean isFinite(Vector3 position) {
        return position != null
            && Float.isFinite(position.x)
            && Float.isFinite(position.y)
            && Float.isFinite(position.z);
    }

    private void ensureRunning() {
        if (!running.get() || engine == null || worldCoordinator == null) {
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
