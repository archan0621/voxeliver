package kr.co.voxeliver.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import kr.co.voxeliver.network.codec.PacketDecoder;
import kr.co.voxeliver.network.codec.PacketEncoder;
import kr.co.voxeliver.network.handler.GamePacketHandler;
import kr.co.voxeliver.network.handler.SimpleConnectHandler;
import kr.co.voxeliver.server.ServerRuntime;

public class GameServerInitializer extends ChannelInitializer<SocketChannel> {
    private final ServerRuntime runtime;

    public GameServerInitializer(ServerRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new SimpleConnectHandler(runtime))
                .addLast(new PacketDecoder())
                .addLast(new GamePacketHandler(runtime))
                .addLast(new PacketEncoder());
    }
}
