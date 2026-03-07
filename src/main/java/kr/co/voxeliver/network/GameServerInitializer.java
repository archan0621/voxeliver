package kr.co.voxeliver.network;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import kr.co.voxeliver.network.handler.SimpleConnectHandler;

public class GameServerInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new LineBasedFrameDecoder(1024))
                .addLast(new StringDecoder())
                .addLast(new StringEncoder())
                .addLast(new SimpleConnectHandler());

    }
}
