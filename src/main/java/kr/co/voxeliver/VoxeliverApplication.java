package kr.co.voxeliver;

import kr.co.voxeliver.network.GameServer;
import kr.co.voxeliver.server.ServerConfig;

public class VoxeliverApplication {

    public static void main(String[] args) {
        GameServer gameServer = new GameServer(ServerConfig.builder().build());
        gameServer.start();
    }
}
