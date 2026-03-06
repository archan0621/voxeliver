package kr.co.voxeliver.server;

import io.netty.channel.nio.NioEventLoopGroup;

public class GameServer {

    private final int port;

    public GameServer(final int port) {
        this.port = port;
    }

}
