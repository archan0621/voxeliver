package kr.co.voxeliver.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.PacketRegistry;

public class PacketDecoder extends ByteToMessageDecoder {
    private static final int PACKET_ID_SIZE = Integer.BYTES;
    private static final int MAX_FRAME_LENGTH = 1024 * 1024;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        in.markReaderIndex();

        int length = in.readInt();
        if (length < PACKET_ID_SIZE) {
            throw new CorruptedFrameException("Invalid frame length: " + length);
        }
        if (length > MAX_FRAME_LENGTH) {
            throw new CorruptedFrameException("Frame exceeds max length: " + length);
        }

        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        ByteBuf frame = in.readRetainedSlice(length);
        try {
            int packetId = frame.readInt();
            Packet packet = PacketRegistry.create(packetId);
            packet.read(frame);

            if (frame.isReadable()) {
                throw new CorruptedFrameException("Packet " + packetId + " left unread bytes: " + frame.readableBytes());
            }

            out.add(packet);
        } finally {
            frame.release();
        }
    }
}
