package io.github.gaming32.worldhost.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;

public class ProxyClient extends Thread {
    private final Socket socket;
    private final InetAddress remoteAddress;
    private final long connectionId;
    private boolean closed;

    public ProxyClient(int port, InetAddress remoteAddress, long connectionId) throws IOException {
        super("ProxyClient for " + connectionId);
        socket = new Socket(InetAddress.getLoopbackAddress(), port);
        this.remoteAddress = remoteAddress;
        this.connectionId = connectionId;
    }

    @Override
    public void run() {
        WorldHostCommon.LOGGER.info("Starting proxy client from {}", remoteAddress);
        try {
            final InputStream is = socket.getInputStream();
            final byte[] b = new byte[24576];
            int n;
            while ((n = is.read(b)) != -1) {
                if (WorldHostCommon.wsClient == null) break;
                if (n == 0) continue;
                WorldHostCommon.wsClient.proxyS2CPacket(connectionId, Arrays.copyOf(b, n));
            }
        } catch (IOException e) {
            WorldHostCommon.LOGGER.error("Proxy client connection for {} has error", remoteAddress, e);
        }
        WorldHostCommon.CONNECTED_PROXY_CLIENTS.remove(connectionId);
        close();
        if (WorldHostCommon.wsClient != null) {
            WorldHostCommon.wsClient.proxyDisconnect(connectionId);
        }
        WorldHostCommon.LOGGER.info("Proxy client connection for {} closed", remoteAddress);
    }

    public void close() {
        if (closed) return;
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            WorldHostCommon.LOGGER.error("Failed to close proxy client socket for {}", remoteAddress, e);
        }
    }

    public OutputStream getOutputStream() throws IOException {
        return socket.getOutputStream();
    }
}
