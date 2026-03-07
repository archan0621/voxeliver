package kr.co.voxeliver.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class GameServer {

    private final int port;

    public GameServer(final int port) {
        this.port = port;
    }

    public void start() {

        try (
                EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
                EventLoopGroup worker = new MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory())
        ) {

            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GameServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("[Server] Starting on port " + port);

            ChannelFuture future = bootstrap.bind(port).sync();

            System.out.println("[Server] Started");

            future.channel().closeFuture().sync();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println("[Server] Shutting down");

    }

}
