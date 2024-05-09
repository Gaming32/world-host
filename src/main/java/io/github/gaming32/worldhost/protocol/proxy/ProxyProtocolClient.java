package io.github.gaming32.worldhost.protocol.proxy;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.WorldHost;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ProxyProtocolClient implements AutoCloseable, ProxyPassthrough {
    private final BlockingQueue<Optional<ProxyMessage>> sendQueue = new LinkedBlockingQueue<>();

    private final String baseAddr;
    private final int mcPort;

    private boolean closed;

    public ProxyProtocolClient(String host, int port, long connectionId, String baseAddr, int mcPort) {
        this.baseAddr = baseAddr;
        this.mcPort = mcPort;
        final Thread connectionThread = new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(host, port);

                final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeLong(connectionId);
                dos.flush();
            } catch (Exception e) {
                WorldHost.LOGGER.error(
                    "Failed to connect to WHEP server {}.",
                    HostAndPort.fromParts(host, port), e
                );
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        WorldHost.LOGGER.error("Failed to close WHEP socket.", e);
                    }
                    socket = null;
                }
            }

            if (socket == null) {
                closed = true;
                return;
            }
            final Socket fSocket = socket;

            final Thread sendThread = new Thread(() -> {
                try {
                    final DataOutputStream dos = new DataOutputStream(fSocket.getOutputStream());
                    while (!closed) {
                        final var message = sendQueue.take();
                        if (message.isEmpty()) break;
                        dos.writeLong(message.get().getConnectionId());
                        dos.writeByte(message.get().getType());
                        message.get().write(dos);
                        dos.flush();
                    }
                } catch (IOException e) {
                    WorldHost.LOGGER.error("Disconnected from WHEP server in send thread.", e);
                } catch (Exception e) {
                    WorldHost.LOGGER.error("Critical error in WHEP send thread", e);
                }
                closed = true;
            }, "WHEP-SendThread");

            final Thread recvThread = new Thread(() -> {
                try {
                    final DataInputStream dis = new DataInputStream(fSocket.getInputStream());
                    while (!closed) {
                        final ProxyMessage message = ProxyMessage.read(dis);
                        switch (message) {
                            case ProxyMessage.Open open ->
                                WorldHost.proxyConnect(open.getConnectionId(), open.getAddress(), () -> WorldHost.proxyProtocolClient);
                            case ProxyMessage.Packet packet ->
                                WorldHost.proxyPacket(packet.getConnectionId(), packet.getBuffer());
                            case ProxyMessage.Close close ->
                                WorldHost.proxyDisconnect(close.getConnectionId());
                        }
                    }
                } catch (Exception e) {
                    if (!(e instanceof SocketException) || !e.getMessage().equals("Socket closed")) {
                        WorldHost.LOGGER.error("Critical error in WHEP recv thread", e);
                    }
                }
                closed = true;
            }, "WHEP-RecvThread");

            sendThread.start();
            recvThread.start();

            try {
                sendThread.join();
            } catch (InterruptedException e) {
                WorldHost.LOGGER.error("{} interrupted.", Thread.currentThread().getName(), e);
            }

            try {
                socket.close();
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to close WHEP socket.", e);
            }
        }, "WHEP-ConnectThread");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    private void enqueue(ProxyMessage message) {
        if (closed) {
            WorldHost.LOGGER.warn("Attempted to send over closed connection: {}", message);
            return;
        }
        try {
            sendQueue.put(Optional.of(message));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void packet(long connectionId, byte[] buffer) {
        enqueue(new ProxyMessage.Packet(connectionId, buffer));
    }

    public void close(long connectionId) {
        enqueue(new ProxyMessage.Close(connectionId));
    }

    @Override
    public void proxyS2CPacket(long connectionId, byte[] data) {
        packet(connectionId, data);
    }

    @Override
    public void proxyDisconnect(long connectionId) {
        close(connectionId);
    }

    public String getBaseAddr() {
        return baseAddr;
    }

    public int getMcPort() {
        return mcPort;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        sendQueue.add(Optional.empty());
    }

    public boolean isClosed() {
        return closed;
    }
}
