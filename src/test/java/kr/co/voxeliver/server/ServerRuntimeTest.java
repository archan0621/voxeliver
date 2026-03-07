package kr.co.voxeliver.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector3;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Path;
import kr.co.voxeliver.network.protocol.impl.BlockUpdatePacket;
import kr.co.voxeliver.network.protocol.impl.BreakBlockRequestPacket;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.protocol.impl.ChunkDataPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import kr.co.voxeliver.network.protocol.impl.PlayerStatePacket;
import kr.co.voxeliver.network.protocol.impl.PlaceBlockRequestPacket;
import kr.co.voxeliver.server.player.ServerPlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerRuntimeTest {

    @TempDir
    Path tempDir;

    @Test
    void startsHeadlessWorldAndCreatesServerPlayers() {
        ServerConfig config = ServerConfig.builder()
            .worldPath(tempDir.resolve("world").toString())
            .initialChunkRadius(1)
            .chunkPreloadRadius(1)
            .visibleChunkRadius(1)
            .keepLoadedChunkRadius(2)
            .maxLoadedChunks(32)
            .build();

        ServerRuntime runtime = new ServerRuntime(config);
        EmbeddedChannel channel = new EmbeddedChannel();

        runtime.start();
        try {
            assertTrue(runtime.isRunning());
            assertNotNull(runtime.getEngine());
            assertNotNull(runtime.getEngine().getWorld());
            assertFalse(runtime.getEngine().getWorld().getChunkManager().getLoadedChunks().isEmpty());

            PlayerSession session = new PlayerSession(channel);
            ServerPlayer player = runtime.login(session, "alice");

            assertNotNull(player);
            assertEquals(session.getPlayerId(), player.getPlayerId());
            assertEquals("alice", player.getUsername());
            assertEquals(player.getPlayerId(), runtime.getPlayer(session).getPlayerId());
            assertEquals(runtime.getSpawnPosition(), player.getPlayer().getPosition());

            runtime.initializePlayerSession(player);

            assertInstanceOf(ChunkDataPacket.class, channel.readOutbound());
        } finally {
            runtime.stop();
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsOversizedMoveAndAcknowledgesRejectedSequence() {
        ServerConfig config = ServerConfig.builder()
            .worldPath(tempDir.resolve("move-world").toString())
            .initialChunkRadius(1)
            .chunkPreloadRadius(1)
            .visibleChunkRadius(1)
            .keepLoadedChunkRadius(2)
            .maxLoadedChunks(32)
            .maxMoveDistancePerRequest(1f)
            .build();

        ServerRuntime runtime = new ServerRuntime(config);
        EmbeddedChannel channel = new EmbeddedChannel();

        runtime.start();
        try {
            PlayerSession session = new PlayerSession(channel);
            ServerPlayer player = runtime.login(session, "alice");
            runtime.initializePlayerSession(player);
            drainOutbound(channel);

            assertFalse(runtime.handleMove(session, new MovePacket(5, 100f, 100f, 100f)));

            PlayerStatePacket statePacket = assertInstanceOf(PlayerStatePacket.class, channel.readOutbound());
            assertEquals(5, statePacket.getAcknowledgedMoveSequence());
            assertEquals(runtime.getSpawnPosition(), statePacket.getPosition());
        } finally {
            runtime.stop();
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void placeAndBreakBlockBroadcastsAuthoritativeUpdates() {
        ServerConfig config = ServerConfig.builder()
            .worldPath(tempDir.resolve("block-world").toString())
            .initialChunkRadius(1)
            .chunkPreloadRadius(1)
            .visibleChunkRadius(1)
            .keepLoadedChunkRadius(2)
            .maxLoadedChunks(32)
            .build();

        ServerRuntime runtime = new ServerRuntime(config);
        EmbeddedChannel channel = new EmbeddedChannel();

        runtime.start();
        try {
            PlayerSession session = new PlayerSession(channel);
            ServerPlayer player = runtime.login(session, "alice");
            runtime.initializePlayerSession(player);
            drainOutbound(channel);

            Vector3 placePosition = new Vector3(2, 1, 0);
            assertTrue(runtime.handlePlaceBlock(session, new PlaceBlockRequestPacket(placePosition, 9)));
            BlockUpdatePacket placeUpdate = assertInstanceOf(BlockUpdatePacket.class, channel.readOutbound());
            assertEquals(placePosition, placeUpdate.getPosition());
            assertEquals(9, placeUpdate.getBlockType());

            Vector3 breakPosition = new Vector3(0, 0, 0);
            assertTrue(runtime.handleBreakBlock(session, new BreakBlockRequestPacket(breakPosition)));
            BlockUpdatePacket breakUpdate = assertInstanceOf(BlockUpdatePacket.class, channel.readOutbound());
            assertEquals(breakPosition, breakUpdate.getPosition());
            assertTrue(breakUpdate.isRemoval());
        } finally {
            runtime.stop();
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void keepsAcceptedJumpPositionsStableAcrossServerTicks() {
        ServerConfig config = ServerConfig.builder()
            .worldPath(tempDir.resolve("jump-world").toString())
            .initialChunkRadius(1)
            .chunkPreloadRadius(1)
            .visibleChunkRadius(1)
            .keepLoadedChunkRadius(2)
            .maxLoadedChunks(32)
            .build();

        ServerRuntime runtime = new ServerRuntime(config);
        EmbeddedChannel channel = new EmbeddedChannel();

        runtime.start();
        try {
            PlayerSession session = new PlayerSession(channel);
            ServerPlayer player = runtime.login(session, "alice");
            runtime.initializePlayerSession(player);
            drainOutbound(channel);

            Vector3 requestedPosition = new Vector3(runtime.getSpawnPosition()).add(0f, 1f, 0f);
            assertTrue(runtime.handleMove(session, new MovePacket(1, requestedPosition.x, requestedPosition.y, requestedPosition.z)));

            runtime.tick();

            assertEquals(requestedPosition, player.getPlayer().getPosition());

            PlayerStatePacket statePacket = assertInstanceOf(PlayerStatePacket.class, channel.readOutbound());
            assertEquals(requestedPosition, statePacket.getPosition());
        } finally {
            runtime.stop();
            channel.finishAndReleaseAll();
        }
    }

    private void drainOutbound(EmbeddedChannel channel) {
        while (channel.readOutbound() != null) {
            // discard bootstrap packets
        }
    }
}
