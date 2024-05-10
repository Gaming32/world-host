package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.function.Supplier;

public final class ProxyClient {
    private final Thread thread;
    private final Socket socket;
    private final InetAddress remoteAddress;
    private final long connectionId;
    private final Supplier<ProxyPassthrough> proxy;

    private boolean closed;

    public ProxyClient(
        int port,
        InetAddress remoteAddress,
        long connectionId,
        Supplier<ProxyPassthrough> proxy
    ) throws IOException {
        thread = Thread.ofVirtual().name("ProxyClient for " + connectionId).unstarted(this::run);
        socket = new Socket(InetAddress.getLoopbackAddress(), port);
        this.remoteAddress = remoteAddress;
        this.connectionId = connectionId;
        this.proxy = proxy;
        if (proxy.get() == null) {
            WorldHost.LOGGER.error("ProxyPassthrough for {} ({}) is initially null.", connectionId, remoteAddress);
        }
    }

    private void run() {
        WorldHost.LOGGER.info("Starting proxy client from {}", remoteAddress);
        try {
            final var is = socket.getInputStream();
            final byte[] b = new byte[0xffff];
            int n;
            while ((n = is.read(b)) != -1) {
                final ProxyPassthrough proxy = this.proxy.get();
                if (proxy == null) break;
                if (n == 0) continue;
                proxy.proxyS2CPacket(connectionId, Arrays.copyOf(b, n));
            }
        } catch (IOException e) {
            WorldHost.LOGGER.error("Proxy client connection for {} has error", remoteAddress, e);
        }
        WorldHost.CONNECTED_PROXY_CLIENTS.remove(connectionId);
        close();
        final ProxyPassthrough proxy = this.proxy.get();
        if (proxy != null) {
            proxy.proxyDisconnect(connectionId);
        }
        WorldHost.LOGGER.info("Proxy client connection for {} closed", remoteAddress);
    }

    public void start() {
        thread.start();
    }

    public void close() {
        if (closed) return;
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            WorldHost.LOGGER.error("Failed to close proxy client socket for {}", remoteAddress, e);
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
}
