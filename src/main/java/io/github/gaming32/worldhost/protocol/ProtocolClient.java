package io.github.gaming32.worldhost.protocol;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.DeferredToastManager;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.toasts.SystemToast;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class ProtocolClient implements AutoCloseable {
    public static final int PROTOCOL_VERSION = 2;

    private final Future<Void> connectingFuture = new CompletableFuture<>();
    private final BlockingQueue<WorldHostC2SMessage> sendQueue = new LinkedBlockingQueue<>();

    private BlockingQueue<UUID> authUuid = new LinkedBlockingQueue<>(1);

    private boolean authenticated, closed;

    private long connectionId = WorldHost.CONNECTION_ID;
    private String baseIp = "";
    private int basePort;
    private String userIp = "";

    public ProtocolClient(String ip, boolean successToast, boolean failureToast) {
        final HostAndPort target = HostAndPort.fromString(ip).withDefaultPort(9646);
        final Thread connectionThread = new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(target.getHost(), target.getPort());

                final UUID userUuid = authUuid.take();
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
                    DeferredToastManager.show(
                        SystemToast.SystemToastIds.TUTORIAL_HINT,
                        Components.translatable("world-host.wh_connect.connect_failed"),
                        Components.immutable(e.getLocalizedMessage())
                    );
                }
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        WorldHost.LOGGER.error("Failed to close WH socket", e1);
                        if (failureToast) {
                            DeferredToastManager.show(
                                SystemToast.SystemToastIds.WORLD_BACKUP,
                                Components.translatable("world-host.wh_connect.close_failed"),
                                Components.immutable(e1.getLocalizedMessage())
                            );
                        }
                    }
                    socket = null;
                }
            }

            if (socket == null) {
                closed = true;
                return;
            }
            if (successToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Components.translatable("world-host.wh_connect.connected"),
                    null
                );
            }
            final Socket fSocket = socket;

            final Thread sendThread = new Thread(() -> {
                try {
                    final DataOutputStream dos = new DataOutputStream(fSocket.getOutputStream());
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final DataOutputStream tempDos = new DataOutputStream(baos);
                    while (!closed) {
                        final WorldHostC2SMessage message = sendQueue.take();
                        if (message == WorldHostC2SMessage.EndMarker.INSTANCE) break;
                        message.encode(tempDos);
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
                closed = true;
            }, "WH-SendThread");

            final Thread recvThread = new Thread(() -> {
                try {
                    final DataInputStream dis = new DataInputStream(fSocket.getInputStream());
                    while (!closed) {
                        final int length = dis.readInt();
                        if (length < 1) {
                            WorldHost.LOGGER.warn("Received invalid short packet (under 1 byte) from WH server");
                            dis.skipNBytes(length);
                            continue;
                        }
                        final BoundedInputStream bis = new BoundedInputStream(dis);
                        bis.setPropagateClose(false);
                        WorldHostS2CMessage.decode(new DataInputStream(bis)).handle(this);
                    }
                } catch (EOFException e) {
                    WorldHost.LOGGER.debug("Recv thread terminated due to socket closure");
                } catch (Exception e) {
                    if (!(e instanceof SocketException) || !closed) {
                        WorldHost.LOGGER.error("Critical error in WH recv thread", e);
                    }
                }
                closed = true;
            }, "WH-RecvThread");

            sendThread.start();
            recvThread.start();

            try {
                sendThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            // recvThread will terminate when the socket is closed, because it's blocking on the socket, not the sendQueue.

            try {
                socket.close();
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to close WH socket.", e);
                if (WorldHost.CONFIG.isEnableReconnectionToasts()) {
                    DeferredToastManager.show(
                        SystemToast.SystemToastIds.WORLD_BACKUP,
                        Components.translatable("world-host.wh_connect.close_failed"),
                        Components.immutable(e.getLocalizedMessage())
                    );
                }
            }
        }, "WH-ConnectionThread");
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    public void authenticate(UUID userUuid) {
        authenticated = true;
        if (authUuid != null) {
            try {
                authUuid.put(userUuid);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
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
            sendQueue.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void listOnline(Collection<UUID> friends) {
        enqueue(new WorldHostC2SMessage.ListOnline(friends));
    }

    public void publishedWorld(Collection<UUID> friends) {
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

    public void requestJoin(UUID friend) {
        enqueue(new WorldHostC2SMessage.RequestJoin(friend));
    }

    public void proxyS2CPacket(long connectionId, byte[] data) {
        enqueue(new WorldHostC2SMessage.ProxyS2CPacket(connectionId, data));
    }

    public void proxyDisconnect(long connectionId) {
        enqueue(new WorldHostC2SMessage.ProxyDisconnect(connectionId));
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

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        sendQueue.add(WorldHostC2SMessage.EndMarker.INSTANCE);
    }
}
