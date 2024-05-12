package io.github.gaming32.worldhost.proxy;

import io.github.gaming32.worldhost.mixin.ServerConnectionListenerAccessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.RateKickingConnection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.LegacyQueryHandler;
import net.minecraft.server.network.ServerConnectionListener;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;

import java.net.SocketAddress;

public class ProxyChannels {
    public static SocketAddress startProxyChannel(ServerConnectionListener listener) {
        final ServerConnectionListenerAccessor accessor = (ServerConnectionListenerAccessor)listener;
        ChannelFuture channel;
        synchronized (accessor.getChannels()) {
            channel = new ServerBootstrap()
                .channel(LocalServerChannel.class)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        final ChannelPipeline pipeline = ch.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                        if (listener.getServer().repliesToStatus()) {
                            pipeline.addLast("legacy_query", new LegacyQueryHandler(listener.getServer()));
                        }
                        Connection.configureSerialization(pipeline, PacketFlow.SERVERBOUND, false, null);
                        final int rateLimit = listener.getServer().getRateLimitPacketsPerSecond();
                        final Connection connection = rateLimit > 0
                            ? new RateKickingConnection(rateLimit)
                            : new Connection(PacketFlow.SERVERBOUND);
                        listener.getConnections().add(connection);
                        connection.configurePacketHandler(pipeline);
                        connection.setListenerForServerboundHandshake(
                            new ServerHandshakePacketListenerImpl(listener.getServer(), connection)
                        );
                    }
                })
                .group(ServerConnectionListener.SERVER_EVENT_GROUP.get())
                .localAddress(LocalAddress.ANY)
                .bind()
                .syncUninterruptibly();
            accessor.getChannels().add(channel);
        }
        return channel.channel().localAddress();
    }
}
