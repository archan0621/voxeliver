package kr.co.voxeliver.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.util.Objects;
import kr.co.voxeliver.network.protocol.Packet;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        Objects.requireNonNull(packet, "packet");
        ByteBuf buf = ctx.alloc().buffer();
        try {
            buf.writeInt(packet.getId());
            packet.write(buf);

            out.writeInt(buf.readableBytes());
            out.writeBytes(buf);
        } finally {
            buf.release();
        }
    }
}
