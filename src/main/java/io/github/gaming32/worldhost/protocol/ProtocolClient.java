package io.github.gaming32.worldhost.protocol;

import com.google.common.net.HostAndPort;
import io.github.gaming32.worldhost.WorldHost;
import org.apache.commons.io.input.BoundedInputStream;

import java.io.*;
import java.net.Socket;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class ProtocolClient implements AutoCloseable {
    private final Future<Void> connectingFuture = new CompletableFuture<>();
    private final BlockingQueue<WorldHostC2SMessage> sendQueue = new LinkedBlockingQueue<>();

    private BlockingQueue<UUID> authUuid = new LinkedBlockingQueue<>(1);

    private boolean authenticated, closed;

    private UUID connectionId = WorldHost.CONNECTION_ID;
    private String baseIp = "";
    private int basePort;

    public ProtocolClient(String ip) {
        final HostAndPort target = HostAndPort.fromString(ip).withDefaultPort(9646);
        final Thread connectionThread = new Thread(() -> {
            Socket socket = null;
            try {
                socket = new Socket(target.getHost(), target.getPort());

                final UUID userUuid = authUuid.take();
                authUuid = null;
                final DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeLong(userUuid.getMostSignificantBits());
                dos.writeLong(userUuid.getLeastSignificantBits());
                dos.writeLong(connectionId.getMostSignificantBits());
                dos.writeLong(connectionId.getLeastSignificantBits());
                dos.flush();
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to connect to {}.", target, e);
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        WorldHost.LOGGER.error("Failed to close socket", e1);
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
                    WorldHost.LOGGER.error("Critical error in WH recv thread", e);
                }
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

    public void proxyDisconnect(long connectionId) {
        enqueue(new WorldHostC2SMessage.ProxyDisconnect(connectionId));
    }

    public Future<Void> getConnectingFuture() {
        return connectingFuture;
    }

    public UUID getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(UUID connectionId) {
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
