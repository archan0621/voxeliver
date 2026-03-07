package kr.co.voxeliver.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import kr.co.voxeliver.server.ServerConfig;
import kr.co.voxeliver.server.ServerRuntime;

public class GameServer {
    private final ServerConfig config;
    private final ServerRuntime runtime;

    public GameServer(final int port) {
        this(ServerConfig.builder().port(port).build());
    }

    public GameServer(ServerConfig config) {
        this.config = config;
        this.runtime = new ServerRuntime(config);
    }

    public void start() {
        try (
                EventLoopGroup boss = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
                EventLoopGroup worker = new MultiThreadIoEventLoopGroup(0, NioIoHandler.newFactory())
        ) {
            runtime.start();

            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new GameServerInitializer(runtime))
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("[Server] Starting on port " + config.port);

            ChannelFuture future = bootstrap.bind(config.port).sync();

            System.out.println("[Server] Started");

            future.channel().closeFuture().sync();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            runtime.stop();
        }

        System.out.println("[Server] Shutting down");
    }
}
