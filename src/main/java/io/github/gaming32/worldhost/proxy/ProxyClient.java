package io.github.gaming32.worldhost.proxy;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.local.LocalChannel;
import net.minecraft.server.network.ServerConnectionListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.function.Supplier;

public final class ProxyClient extends SimpleChannelInboundHandler<ByteBuf> {
    private static final int PACKET_SIZE = 0xffff;

    private final InetAddress remoteAddress;
    private final long connectionId;
    private final Supplier<ProxyPassthrough> proxy;

    private ByteArrayOutputStream preActiveBuffer = new ByteArrayOutputStream();
    private Channel channel;
    private boolean closed;

    public ProxyClient(
        InetAddress remoteAddress,
        long connectionId,
        Supplier<ProxyPassthrough> proxy
    ) throws IOException {
        this.remoteAddress = remoteAddress;
        this.connectionId = connectionId;
        this.proxy = proxy;
        if (proxy.get() == null) {
            WorldHost.LOGGER.error("ProxyPassthrough for {} ({}) is initially null.", connectionId, remoteAddress);
        }
    }

    @Override
    public synchronized void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        channel = ctx.channel();
        send(preActiveBuffer.toByteArray());
        preActiveBuffer = null;
        WorldHost.LOGGER.info("Started proxy client from {}", remoteAddress);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        WorldHost.CONNECTED_PROXY_CLIENTS.remove(connectionId);
        close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        WorldHost.LOGGER.error("Proxy client connection for {} had error", remoteAddress, cause);
        WorldHost.CONNECTED_PROXY_CLIENTS.remove(connectionId);
        close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        final ProxyPassthrough proxy = this.proxy.get();
        if (proxy == null) {
            close();
            return;
        }

        while (true) {
            int len = Math.min(msg.readableBytes(), PACKET_SIZE);
            if (len == 0) break;
            final byte[] buffer = new byte[len];
            msg.readBytes(buffer);
            proxy.proxyS2CPacket(connectionId, buffer);
        }
    }

    public void start() {
        WorldHost.LOGGER.info("Starting proxy client from {}", remoteAddress);
        new Bootstrap()
            .group(ServerConnectionListener.SERVER_EVENT_GROUP.get())
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline().addLast("handler", ProxyClient.this);
                }
            })
            .channel(LocalChannel.class)
            .connect(WorldHost.proxySocketAddress)
            .syncUninterruptibly();
    }

    public void close() {
        if (closed) return;
        closed = true;
        try {
            channel.close();
            final ProxyPassthrough proxy = this.proxy.get();
            if (proxy != null) {
                proxy.proxyDisconnect(connectionId);
            }
            WorldHost.LOGGER.info("Proxy client connection for {} closed", remoteAddress);
        } catch (Exception e) {
            WorldHost.LOGGER.error("Proxy client connection for {} failed to close", remoteAddress, e);
        }
    }

    public synchronized void send(byte[] message) {
        if (channel == null) {
            preActiveBuffer.writeBytes(message);
            return;
        }
        if (channel.eventLoop().inEventLoop()) {
            doSend(message);
        } else {
            channel.eventLoop().execute(() -> doSend(message));
        }
    }

    private void doSend(byte[] message) {
        channel.writeAndFlush(Unpooled.wrappedBuffer(message))
            .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }
}
