package kr.co.voxeliver.server;

import com.badlogic.gdx.math.Vector3;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.co.voxelite.engine.VoxeliteEngine;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxelite.world.ChunkManager;
import kr.co.voxeliver.network.protocol.impl.BlockUpdatePacket;
import kr.co.voxeliver.network.protocol.impl.ChunkDataPacket;
import kr.co.voxeliver.network.protocol.impl.ChunkUnloadPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerJoinedPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerLeftPacket;
import kr.co.voxeliver.network.protocol.impl.PlayerStatePacket;
import kr.co.voxeliver.server.player.ServerPlayer;

class ServerWorldCoordinator {
    private final ServerConfig config;
    private final VoxeliteEngine engine;
    private final ServerPlayerRegistry playerRegistry;

    public ServerWorldCoordinator(ServerConfig config, VoxeliteEngine engine, ServerPlayerRegistry playerRegistry) {
        this.config = config;
        this.engine = engine;
        this.playerRegistry = playerRegistry;
    }

    public void initializePlayerSession(ServerPlayer player) {
        if (player == null) {
            return;
        }

        for (ServerPlayer other : playerRegistry.snapshot()) {
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

    public void updateWorldStreaming(Vector3 fallbackSpawnPosition) {
        List<Vector3> playerPositions = new ArrayList<>();
        for (ServerPlayer player : playerRegistry.snapshot()) {
            playerPositions.add(new Vector3(player.getPlayer().getPosition()));
        }

        if (playerPositions.isEmpty()) {
            playerPositions.add(new Vector3(fallbackSpawnPosition));
        }

        engine.getWorld().updateChunks(playerPositions);
        engine.getWorld().processPendingChunks();
    }

    public void syncChunksForAllPlayers() {
        for (ServerPlayer player : playerRegistry.snapshot()) {
            syncChunksForPlayer(player);
        }
    }

    public void syncChunksForPlayer(ServerPlayer player) {
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

    public void broadcastPlayerStates() {
        List<ServerPlayer> snapshot = new ArrayList<>(playerRegistry.snapshot());
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

    public void broadcastPlayerJoined(ServerPlayer joinedPlayer) {
        for (ServerPlayer recipient : playerRegistry.snapshot()) {
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

    public void broadcastPlayerLeft(ServerPlayer removedPlayer) {
        PlayerLeftPacket packet = new PlayerLeftPacket(removedPlayer.getPlayerId());
        for (ServerPlayer recipient : playerRegistry.snapshot()) {
            recipient.getSession().send(packet);
        }
    }

    public void broadcastBlockUpdate(BlockUpdatePacket packet) {
        for (ServerPlayer recipient : playerRegistry.snapshot()) {
            recipient.getSession().send(packet);
        }
    }

    public void sendAuthoritativePlayerState(ServerPlayer player) {
        player.getSession().send(new PlayerStatePacket(
            player.getPlayerId(),
            player.getLastProcessedMoveSequence(),
            player.getPlayer().getPosition()
        ));
    }

    public void sendAuthoritativeBlockState(ServerPlayer player, Vector3 blockPosition) {
        int blockType = engine.getWorld().getBlockType(blockPosition);
        player.getSession().send(new BlockUpdatePacket(blockPosition, blockType));
    }

    public void invalidateAllPhysicsCaches() {
        if (engine.getPhysics() != null) {
            engine.getPhysics().invalidateCache();
        }

        for (ServerPlayer player : playerRegistry.snapshot()) {
            player.getPhysics().invalidateCache();
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
}
