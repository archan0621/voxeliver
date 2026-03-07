package kr.co.voxeliver;

import kr.co.voxeliver.network.GameServer;

public class VoxeliverApplication {

    public static void main(String[] args) {
        System.out.println("Hello World");

        GameServer gameServer = new GameServer(25565);
        gameServer.start();
    }
}
