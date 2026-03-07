package kr.co.voxeliver.network.channel;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

public class ServerChannels {

    public static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);


}
