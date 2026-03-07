package kr.co.voxeliver.network.codec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.DecoderException;
import kr.co.voxelite.world.Chunk;
import kr.co.voxelite.world.ChunkCoord;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.impl.ChunkDataPacket;
import kr.co.voxeliver.network.protocol.impl.ChatPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import org.junit.jupiter.api.Test;

class PacketCodecTest {

    @Test
    void roundTripsChatPacket() {
        EmbeddedChannel encoder = new EmbeddedChannel(new PacketEncoder());
        EmbeddedChannel decoder = new EmbeddedChannel(new PacketDecoder());

        try {
            ChatPacket packet = new ChatPacket("hello");

            assertTrue(encoder.writeOutbound(packet));
            ByteBuf encoded = encoder.readOutbound();
            try {
                assertTrue(decoder.writeInbound(encoded.retainedDuplicate()));
                Packet decoded = decoder.readInbound();

                ChatPacket chatPacket = assertInstanceOf(ChatPacket.class, decoded);
                assertEquals("hello", chatPacket.getMessage());
            } finally {
                encoded.release();
            }
        } finally {
            encoder.finishAndReleaseAll();
            decoder.finishAndReleaseAll();
        }
    }

    @Test
    void waitsForCompleteFrameBeforeDecoding() {
        EmbeddedChannel encoder = new EmbeddedChannel(new PacketEncoder());
        EmbeddedChannel decoder = new EmbeddedChannel(new PacketDecoder());

        try {
            assertTrue(encoder.writeOutbound(new MovePacket(7, 1.0f, 2.0f, 3.0f)));
            ByteBuf encoded = encoder.readOutbound();
            try {
                ByteBuf firstHalf = encoded.copy(0, 6);
                ByteBuf secondHalf = encoded.copy(6, encoded.readableBytes() - 6);

                assertFalse(decoder.writeInbound(firstHalf));
                assertTrue(decoder.writeInbound(secondHalf));

                MovePacket decoded = assertInstanceOf(MovePacket.class, decoder.readInbound());
                assertEquals(7, decoded.getSequence());
                assertEquals(1.0f, decoded.getX());
                assertEquals(2.0f, decoded.getY());
                assertEquals(3.0f, decoded.getZ());
            } finally {
                encoded.release();
            }
        } finally {
            encoder.finishAndReleaseAll();
            decoder.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsUnknownPacketIds() {
        EmbeddedChannel decoder = new EmbeddedChannel(new PacketDecoder());
        ByteBuf frame = Unpooled.buffer();
        frame.writeInt(Integer.BYTES);
        frame.writeInt(999);

        try {
            assertThrows(DecoderException.class, () -> decoder.writeInbound(frame.retainedDuplicate()));
        } finally {
            frame.release();
            decoder.finishAndReleaseAll();
        }
    }

    @Test
    void roundTripsChunkDataPacket() {
        EmbeddedChannel encoder = new EmbeddedChannel(new PacketEncoder());
        EmbeddedChannel decoder = new EmbeddedChannel(new PacketDecoder());

        try {
            Chunk chunk = new Chunk(new ChunkCoord(1, -2));
            chunk.addBlockLocal(0, 1, 0, 3);
            chunk.addBlockLocal(5, 2, 7, 4);
            chunk.markAsGenerated();

            assertTrue(encoder.writeOutbound(new ChunkDataPacket(chunk)));
            ByteBuf encoded = encoder.readOutbound();
            try {
                assertTrue(decoder.writeInbound(encoded.retainedDuplicate()));
                ChunkDataPacket decoded = assertInstanceOf(ChunkDataPacket.class, decoder.readInbound());

                assertEquals(new ChunkCoord(1, -2), decoded.getChunkCoord());
                Chunk decodedChunk = decoded.toChunk();
                assertEquals(2, decodedChunk.getBlocks().size());
                assertTrue(decodedChunk.isGenerated());
            } finally {
                encoded.release();
            }
        } finally {
            encoder.finishAndReleaseAll();
            decoder.finishAndReleaseAll();
        }
    }
}
