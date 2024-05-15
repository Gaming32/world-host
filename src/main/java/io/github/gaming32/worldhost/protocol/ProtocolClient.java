package io.github.gaming32.worldhost.protocol;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.protocol.proxy.ProxyPassthrough;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.versions.Components;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.CountingInputStream;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public final class ProtocolClient implements AutoCloseable, ProxyPassthrough {
    public static final int PROTOCOL_VERSION = 5;

    private final String originalHost;
    final CompletableFuture<Void> connectingFuture = new CompletableFuture<>();
    private final BlockingQueue<Optional<WorldHostC2SMessage>> sendQueue = new LinkedBlockingQueue<>();

    private CompletableFuture<UUID> authUuid = new CompletableFuture<>();

    private boolean authenticated, closed;

    private long connectionId = WorldHost.CONNECTION_ID;
    private String baseIp = "";
    private int basePort;
    private String userIp = "";
    private int punchPort;

    @Nullable
    private Long attemptingToJoin;

    public ProtocolClient(String host, boolean successToast, boolean failureToast) {
        this.originalHost = host;
        final HostAndPort target = HostAndPort.fromString(host).withDefaultPort(9646);
        Thread.ofVirtual().name("WH-ConnectionThread").start(() -> {
            Socket socket = null;
            try {
                socket = new Socket(target.getHost(), target.getPort());

                final UUID userUuid = authUuid.join();
                authUuid = null;
                final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeInt(PROTOCOL_VERSION);
                dos.writeLong(userUuid.getMostSignificantBits());
                dos.writeLong(userUuid.getLeastSignificantBits());
                dos.writeLong(connectionId);
                dos.flush();
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to connect to {}.", target, e);
                if (failureToast) {
                    WHToast.builder("world-host.wh_connect.connect_failed")
                        .description(Components.nullToEmpty(e.getLocalizedMessage()))
                        .show();
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        WorldHost.LOGGER.error("Failed to close WH socket", e1);
                        if (failureToast) {
                            WHToast.builder("world-host.wh_connect.close_failed")
                                .description(Components.nullToEmpty(e1.getLocalizedMessage()))
                                .show();
                        }
                    }
                    socket = null;
                }
            }

            if (socket == null) {
                close();
                return;
            }
            if (successToast) {
                WHToast.builder("world-host.wh_connect.connected").show();
            }
            final Socket fSocket = socket;

            final Thread sendThread = Thread.ofVirtual().name("WH-SendThread").start(() -> {
                try {
                    final DataOutputStream dos = new DataOutputStream(fSocket.getOutputStream());
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final DataOutputStream tempDos = new DataOutputStream(baos);
                    while (!closed) {
                        final var message = sendQueue.take();
                        if (message.isEmpty()) break;
                        message.get().encode(tempDos);
                        dos.writeInt(baos.size());
                        dos.write(baos.toByteArray());
                        baos.reset();
                        dos.flush();
                    }
                } catch (IOException e) {
                    WorldHost.LOGGER.error("Disconnected from WH server in send thread", e);
                } catch (Exception e) {
                    WorldHost.LOGGER.error("Critical error in WH send thread", e);
                }
                close();
            });

            Thread.ofVirtual().name("WH-RecvThread").start(() -> {
                try {
                    final DataInputStream dis = new DataInputStream(fSocket.getInputStream());
                    while (!closed) {
                        final int length = dis.readInt();
                        if (length < 1) {
                            WorldHost.LOGGER.warn("Received invalid short packet (under 1 byte) from WH server");
                            dis.skipNBytes(length);
                            continue;
                        }
                        final BoundedInputStream bis = new BoundedInputStream(dis, length);
                        bis.setPropagateClose(false);
                        final CountingInputStream cis = new CountingInputStream(bis);
                        WorldHostS2CMessage message = null;
                        try {
                            message = WorldHostS2CMessage.decode(new DataInputStream(cis));
                        } catch (EOFException e) {
                            WorldHost.LOGGER.error("Message decoder read past end (length {})!", length);
                        } catch (Exception e) {
                            WorldHost.LOGGER.error("Error decoding WH message", e);
                        }
                        if (cis.getCount() < length) {
                            WorldHost.LOGGER.warn(
                                "Didn't read entire message (read: {}, total: {}, message: {})",
                                cis.getCount(), length, message
                            );
                            dis.skipNBytes(length - cis.getCount());
                        }
                        if (message == null) continue; // An error occurred!
                        WorldHost.LOGGER.debug("Received {}", message);
                        message.handle(this);
                    }
                } catch (Exception e) {
                    if (!(e instanceof SocketException) || !e.getMessage().equals("Socket closed")) {
                        WorldHost.LOGGER.error("Critical error in WH recv thread", e);
                    }
                }
                close();
            });

            try {
                sendThread.join();
            } catch (InterruptedException e) {
                WorldHost.LOGGER.error("{} interrupted", Thread.currentThread().getName(), e);
            }

            // recvThread will terminate when the socket is closed, because it's blocking on the socket, not the sendQueue.

            try {
                socket.close();
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to close WH socket.", e);
                if (WorldHost.CONFIG.isEnableReconnectionToasts()) {
                    WHToast.builder("world-host.wh_connect.close_failed")
                        .description(Components.nullToEmpty(e.getLocalizedMessage()))
                        .show();
                }
            }
        });
    }

    public String getOriginalHost() {
        return originalHost;
    }

    public void authenticate(UUID userUuid) {
        authenticated = true;
        if (authUuid != null) {
            authUuid.complete(userUuid);
        }
    }

    void enqueue(WorldHostC2SMessage message) {
        if (closed) {
            WorldHost.LOGGER.warn("Attempted to send over closed connection: {}", message);
            return;
        }
        if (!authenticated) {
            throw new IllegalStateException("Attempted to communicate with server before authenticating.");
        }
        try {
            sendQueue.put(Optional.of(message));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void listOnline(Collection<UUID> friends) {
        enqueue(new WorldHostC2SMessage.ListOnline(friends));
    }

    public void publishedWorld(Collection<UUID> friends) {
        WorldHost.LOGGER.info("Published world to friends: {}", friends);
        enqueue(new WorldHostC2SMessage.PublishedWorld(friends));
    }

    public void closedWorld(Collection<UUID> friends) {
        enqueue(new WorldHostC2SMessage.ClosedWorld(friends));
    }

    public void friendRequest(UUID friend) {
        enqueue(new WorldHostC2SMessage.FriendRequest(friend));
    }

    public void queryRequest(Collection<UUID> friends) {
        enqueue(new WorldHostC2SMessage.QueryRequest(friends));
    }

    @Deprecated
    public void requestJoin(UUID friend) {
        enqueue(new WorldHostC2SMessage.RequestJoin(friend));
    }

    @Override
    public void proxyS2CPacket(long connectionId, byte[] data) {
        enqueue(new WorldHostC2SMessage.ProxyS2CPacket(connectionId, data));
    }

    @Override
    public void proxyDisconnect(long connectionId) {
        enqueue(new WorldHostC2SMessage.ProxyDisconnect(connectionId));
    }

    public void requestDirectJoin(long connectionId) {
        enqueue(new WorldHostC2SMessage.RequestDirectJoin(connectionId));
    }

    public Future<Void> getConnectingFuture() {
        return connectingFuture;
    }

    public long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(long connectionId) {
        this.connectionId = connectionId;
    }

    public String getBaseIp() {
        return baseIp;
    }

    public void setBaseIp(String baseIp) {
        this.baseIp = baseIp;
    }

    public int getBasePort() {
        return basePort;
    }

    public void setBasePort(int basePort) {
        this.basePort = basePort;
    }

    public String getUserIp() {
        return userIp;
    }

    public void setUserIp(String userIp) {
        this.userIp = userIp;
    }

    public int getPunchPort() {
        return punchPort;
    }

    public void setPunchPort(int punchPort) {
        this.punchPort = punchPort;
    }

    @Nullable
    public Long getAttemptingToJoin() {
        return attemptingToJoin;
    }

    public void setAttemptingToJoin(@Nullable Long attemptingToJoin) {
        this.attemptingToJoin = attemptingToJoin;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        sendQueue.add(Optional.empty());
    }
}
