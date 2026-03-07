package kr.co.voxeliver.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kr.co.voxeliver.network.session.PlayerSession;
import kr.co.voxeliver.network.session.SessionAttributes;
import kr.co.voxeliver.network.session.SessionManager;
import kr.co.voxeliver.server.ServerRuntime;

public class SimpleConnectHandler extends ChannelInboundHandlerAdapter {
    private final ServerRuntime runtime;

    private PlayerSession session;

    public SimpleConnectHandler(ServerRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[CONNECT] " + ctx.channel().remoteAddress());

        session = new PlayerSession(ctx.channel());
        ctx.channel().attr(SessionAttributes.SESSION).set(session);
        SessionManager.add(session);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[DISCONNECT] " + ctx.channel().remoteAddress());

        runtime.disconnect(session);
        SessionManager.remove(session);
        ctx.channel().attr(SessionAttributes.SESSION).set(null);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("[CONNECT] " + cause.getMessage());
        ctx.close();
    }
}
