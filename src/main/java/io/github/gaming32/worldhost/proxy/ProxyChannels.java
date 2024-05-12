package io.github.gaming32.worldhost.proxy;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.mixin.ServerConnectionListenerAccessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import net.minecraft.server.network.ServerConnectionListener;

import java.net.SocketAddress;

public class ProxyChannels {
    public static SocketAddress startProxyChannel(ServerConnectionListener listener) {
        final ServerConnectionListenerAccessor accessor = (ServerConnectionListenerAccessor)listener;
        ChannelFuture channel;
        synchronized (accessor.getChannels()) {
            channel = new ServerBootstrap()
                .channel(LocalServerChannel.class)
                .childHandler(WorldHost.createChannelInitializer(listener))
                .group(ServerConnectionListener.SERVER_EVENT_GROUP.get())
                .localAddress(LocalAddress.ANY)
                .bind()
                .syncUninterruptibly();
            accessor.getChannels().add(channel);
        }
        return channel.channel().localAddress();
    }
}
