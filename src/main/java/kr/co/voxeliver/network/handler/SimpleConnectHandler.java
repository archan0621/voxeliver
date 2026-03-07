package kr.co.voxeliver.network.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import kr.co.voxeliver.network.channel.ServerChannels;

public class SimpleConnectHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("[CONNECT] " + ctx.channel().remoteAddress());

        ServerChannels.channels.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("[DISCONNECT] " + ctx.channel().remoteAddress());

        ServerChannels.channels.remove(ctx.channel());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        String message = (String) msg;
        System.out.println("[RECV] " + message);

        ServerChannels.channels.writeAndFlush(message + "\n");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("[ERROR] " + cause.getMessage());
        ctx.close();

    }
}