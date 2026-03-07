package kr.co.voxeliver.network.protocol;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;

public final class PacketData {
    private PacketData() {
    }

    public static String readString(ByteBuf buf, int maxLength) {
        int length = buf.readInt();
        if (length < 0 || length > maxLength) {
            throw new IllegalArgumentException("Invalid string length: " + length);
        }
        if (length > buf.readableBytes()) {
            throw new IllegalArgumentException("String payload exceeds frame length");
        }

        byte[] data = new byte[length];
        buf.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buf, String value, int maxLength) {
        byte[] data = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (data.length > maxLength) {
            throw new IllegalArgumentException("String payload too large: " + data.length);
        }

        buf.writeInt(data.length);
        buf.writeBytes(data);
    }
}
