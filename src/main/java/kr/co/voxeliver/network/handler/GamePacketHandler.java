package kr.co.voxeliver.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import kr.co.voxeliver.network.protocol.Packet;
import kr.co.voxeliver.network.protocol.impl.BreakBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.LoginAcceptedPacket;
import kr.co.voxeliver.network.protocol.impl.LoginRequestPacket;
import kr.co.voxeliver.network.protocol.impl.MovePacket;
import kr.co.voxeliver.network.protocol.impl.PlaceBlockRequestPacket;
import kr.co.voxeliver.network.protocol.impl.PingPacket;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.session.SessionAttributes;
import kr.co.voxeliver.server.ServerRuntime;
import kr.co.voxeliver.server.player.ServerPlayer;

public class GamePacketHandler extends SimpleChannelInboundHandler<Packet> {
    private final ServerRuntime runtime;

    public GamePacketHandler(ServerRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
        PlayerSession session = ctx.channel().attr(SessionAttributes.SESSION).get();
        if (session == null) {
            ctx.close();
            return;
        }

        if (packet instanceof LoginRequestPacket loginRequestPacket) {
            ServerPlayer player = runtime.login(session, loginRequestPacket.getUsername());
            session.send(new LoginAcceptedPacket(player.getPlayerId(), player.getPlayer().getPosition()));
            runtime.initializePlayerSession(player);
            return;
        }

        if (packet instanceof PingPacket pingPacket) {
            System.out.println("Ping received: " + pingPacket.toString());
            runtime.touch(session);
            session.send(new PingPacket());
        }

        if (packet instanceof MovePacket movePacket) {
            if (runtime.getPlayer(session) == null) {
                System.out.println("[PACKET] Ignoring movement before login");
                return;
            }
            runtime.handleMove(session, movePacket);
            return;
        }

        if (packet instanceof BreakBlockRequestPacket breakBlockRequestPacket) {
            runtime.handleBreakBlock(session, breakBlockRequestPacket);
            return;
        }

        if (packet instanceof PlaceBlockRequestPacket placeBlockRequestPacket) {
            runtime.handlePlaceBlock(session, placeBlockRequestPacket);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[PACKET] " + cause.getMessage());
        ctx.close();
    }
}
