package kr.co.voxeliver.network.session;

import io.netty.channel.Channel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import kr.co.voxeliver.network.protocol.Packet;

public class PlayerSession {

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    private final int playerId;
    private final Channel channel;

    public PlayerSession(Channel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.playerId = NEXT_ID.getAndIncrement();
    }

    public Channel getChannel() {
        return this.channel;
    }

    public int getPlayerId() {
        return this.playerId;
    }

    public void send(Packet packet) {
        channel.writeAndFlush(packet);
    }
}
