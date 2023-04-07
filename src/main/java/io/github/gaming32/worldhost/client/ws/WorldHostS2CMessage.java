package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.FriendsListUpdate;
import io.github.gaming32.worldhost.client.WorldHostClient;
import io.github.gaming32.worldhost.upnp.UPnPErrors;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostS2CMessage.kt
public sealed interface WorldHostS2CMessage {
    record Error(String message) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            WorldHost.LOGGER.error("Received protocol error: {}", message);
        }
    }

    record IsOnlineTo(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(user)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null && server.isPublished()) {
                    session.getAsyncRemote().sendObject(
                        new WorldHostC2SMessage.PublishedWorld(List.of(user))
                    );
                }
            }
        }
    }

    record OnlineGame(String host, int port) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            Minecraft.getInstance().execute(() -> {
                final Minecraft client = Minecraft.getInstance();
                ConnectScreen.startConnecting(client.screen, client, new ServerAddress(host, port), null);
            });
        }
    }

    record FriendRequest(UUID fromUser) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            WorldHostClient.showProfileToast(
                fromUser, "world-host.friend_added_you",
                WorldHostData.friends.contains(fromUser) ? null : Component.translatable("world-host.need_add_back")
            );
        }
    }

    record PublishedWorld(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (!WorldHostData.friends.contains(user)) return;
            WorldHostClient.ONLINE_FRIENDS.add(user);
            WorldHostClient.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
            WorldHostClient.showProfileToast(
                user, "world-host.went_online",
                Component.translatable("world-host.went_online.desc")
            );
        }
    }

    record ClosedWorld(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            WorldHostClient.ONLINE_FRIENDS.remove(user);
            WorldHostClient.ONLINE_FRIEND_PINGS.remove(user);
            WorldHostClient.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        }
    }

    record RequestJoin(UUID user, UUID connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(user)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server == null || !server.isPublished()) return;
                if (WorldHostClient.upnpGateway != null) {
                    try {
                        final UPnPErrors.AddPortMappingErrors error = WorldHostClient.upnpGateway.openPort(
                            server.getPort(), 60, false
                        );
                        if (error == null) {
                            session.getAsyncRemote().sendObject(new WorldHostC2SMessage.JoinGranted(
                                connectionId, new JoinType.UPnP(server.getPort())
                            ));
                            return;
                        }
                        WorldHost.LOGGER.info("Failed to use UPnP mode due to {}. Falling back to Proxy mode.", error);
                    } catch (Exception e) {
                        WorldHost.LOGGER.error("Failed to open UPnP due to exception", e);
                    }
                }
                session.getAsyncRemote().sendObject(new WorldHostC2SMessage.JoinGranted(
                    connectionId, JoinType.Proxy.INSTANCE
                ));
            }
        }
    }

    record QueryRequest(UUID friend, UUID connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(friend)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    session.getAsyncRemote().sendObject(new WorldHostC2SMessage.QueryResponse(
                        connectionId, server.getStatus()
                    ));
                }
            }
        }
    }

    record QueryResponse(UUID friend, ServerStatus metadata) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(friend)) {
                WorldHostClient.ONLINE_FRIEND_PINGS.put(friend, metadata);
            }
        }
    }

    // TODO: Implement using a proper Netty channel to introduce packets directly to the Netty pipeline somehow.
    record ProxyC2SPacket(long connectionId, byte[] data) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            final ProxyClient client = WorldHostClient.CONNECTED_PROXY_CLIENTS.get(connectionId);
            if (client != null) {
                try {
                    client.getOutputStream().write(data);
                } catch (IOException e) {
                    WorldHost.LOGGER.error("Failed to write to ProxyClient", e);
                }
            }
        }
    }

    record ProxyConnect(long connectionId, InetAddress remoteAddr) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null || !server.isPublished()) {
                if (WorldHostClient.wsClient != null) {
                    WorldHostClient.wsClient.proxyDisconnect(connectionId);
                }
                return;
            }
            try {
                final ProxyClient client = new ProxyClient(server.getPort(), remoteAddr, connectionId);
                WorldHostClient.CONNECTED_PROXY_CLIENTS.put(connectionId, client);
                client.start();
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to start ProxyClient", e);
            }
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            final ProxyClient client = WorldHostClient.CONNECTED_PROXY_CLIENTS.remove(connectionId);
            if (client != null) {
                client.close();
            }
        }
    }

    void handle(Session session);

    static WorldHostS2CMessage decode(DataInputStream dis) throws IOException {
        final int typeId = dis.readUnsignedByte();
        return switch (typeId) {
            case 0 -> new Error(readString(dis));
            case 1 -> new IsOnlineTo(readUuid(dis));
            case 2 -> new OnlineGame(readString(dis), dis.readUnsignedShort());
            case 3 -> new FriendRequest(readUuid(dis));
            case 4 -> new PublishedWorld(readUuid(dis));
            case 5 -> new ClosedWorld(readUuid(dis));
            case 6 -> new RequestJoin(readUuid(dis), readUuid(dis));
            case 7 -> new QueryRequest(readUuid(dis), readUuid(dis));
            case 8 -> {
                final UUID friend = readUuid(dis);
                final FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeBytes(dis, dis.readInt());
                yield new QueryResponse(
                    friend,
                    new ClientboundStatusResponsePacket(buf).getStatus()
                );
            }
            case 9 -> new ProxyC2SPacket(dis.readLong(), dis.readAllBytes());
            case 10 -> new ProxyConnect(dis.readLong(), InetAddress.getByAddress(dis.readNBytes(dis.readUnsignedByte())));
            case 11 -> new ProxyDisconnect(dis.readLong());
            default -> new Error("Received packet with unknown type_id from server: " + typeId);
        };
    }

    static UUID readUuid(DataInputStream dis) throws IOException {
        return new UUID(dis.readLong(), dis.readLong());
    }

    static String readString(DataInputStream dis) throws IOException {
        final byte[] buf = new byte[dis.readUnsignedShort()];
        dis.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    class Decoder implements javax.websocket.Decoder.BinaryStream<WorldHostS2CMessage> {
        @Override
        public WorldHostS2CMessage decode(InputStream is) throws IOException {
            return WorldHostS2CMessage.decode(new DataInputStream(is));
        }

        @Override
        public void init(EndpointConfig config) {
        }

        @Override
        public void destroy() {
        }
    }
}
