package io.github.gaming32.worldhost.protocol;

import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.upnp.UPnPErrors;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostS2CMessage.kt
public sealed interface WorldHostS2CMessage {
    record Error(String message) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.error("Received protocol error: {}", message);
        }
    }

    record IsOnlineTo(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(user)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null && server.isPublished()) {
                    client.publishedWorld(List.of(user));
                }
            }
        }
    }

    record OnlineGame(String host, int port) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> {
                final Minecraft mcClient = Minecraft.getInstance();
                assert mcClient.screen != null;
                //noinspection DataFlowIssue // IntelliJ, it's literally marked @Nullable :clown:
                ConnectScreen.startConnecting(mcClient.screen, mcClient, new ServerAddress(host, port), null);
            });
        }
    }

    record FriendRequest(UUID fromUser) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (!WorldHost.CONFIG.isEnableFriends()) return;
            WorldHost.showProfileToast(
                fromUser, "world-host.friend_added_you",
                WorldHost.isFriend(fromUser) ? null : Components.translatable("world-host.need_add_back")
            );
        }
    }

    record PublishedWorld(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (!WorldHost.isFriend(user)) return;
            WorldHost.ONLINE_FRIENDS.add(user);
            WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
            WorldHost.showProfileToast(
                user, "world-host.went_online",
                Components.translatable("world-host.went_online.desc")
            );
        }
    }

    record ClosedWorld(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.ONLINE_FRIENDS.remove(user);
            WorldHost.ONLINE_FRIEND_PINGS.remove(user);
            WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        }
    }

    record RequestJoin(UUID user, UUID connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(user)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server == null || !server.isPublished()) return;
                if (WorldHost.upnpGateway != null) {
                    try {
                        final UPnPErrors.AddPortMappingErrors error = WorldHost.upnpGateway.openPort(
                            server.getPort(), 60, false
                        );
                        if (error == null) {
                            client.enqueue(new WorldHostC2SMessage.JoinGranted(
                                connectionId, new JoinType.UPnP(server.getPort())
                            ));
                            return;
                        }
                        WorldHost.LOGGER.info("Failed to use UPnP mode due to {}. Falling back to Proxy mode.", error);
                    } catch (Exception e) {
                        WorldHost.LOGGER.error("Failed to open UPnP due to exception", e);
                    }
                }
                client.enqueue(new WorldHostC2SMessage.JoinGranted(connectionId, JoinType.Proxy.INSTANCE));
            }
        }
    }

    record QueryRequest(UUID friend, UUID connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(friend)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    client.enqueue(new WorldHostC2SMessage.QueryResponse(connectionId, server.getStatus()));
                }
            }
        }
    }

    record QueryResponse(UUID friend, ServerStatus metadata) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(friend)) {
                WorldHost.ONLINE_FRIEND_PINGS.put(friend, metadata);
            }
        }
    }

    // TODO: Implement using a proper Netty channel to introduce packets directly to the Netty pipeline somehow.
    record ProxyC2SPacket(long connectionId, byte[] data) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            final ProxyClient proxyClient = WorldHost.CONNECTED_PROXY_CLIENTS.get(connectionId);
            if (proxyClient != null) {
                try {
                    proxyClient.getOutputStream().write(data);
                } catch (IOException e) {
                    WorldHost.LOGGER.error("Failed to write to ProxyClient", e);
                }
            }
        }
    }

    record ProxyConnect(long connectionId, InetAddress remoteAddr) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null || !server.isPublished()) {
                if (client != null) {
                    client.proxyDisconnect(connectionId);
                }
                return;
            }
            try {
                final ProxyClient proxyClient = new ProxyClient(server.getPort(), remoteAddr, connectionId);
                WorldHost.CONNECTED_PROXY_CLIENTS.put(connectionId, proxyClient);
                proxyClient.start();
            } catch (IOException e) {
                WorldHost.LOGGER.error("Failed to start ProxyClient", e);
            }
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            final ProxyClient proxyClient = WorldHost.CONNECTED_PROXY_CLIENTS.remove(connectionId);
            if (proxyClient != null) {
                proxyClient.close();
            }
        }
    }

    record ConnectionInfo(UUID connectionId, String baseIp, int basePort) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            client.setConnectionId(connectionId);
            client.setBaseIp(baseIp);
            client.setBasePort(basePort);
        }
    }

    /**
     * NOTE: This method is called from the RecvThread, so it should be careful to not do anything that could
     * <ol>
     *   <li>Cause race conditions (as such, it should not call very much Minecraft code)</li>
     *   <li>Take too long, as that will delay the operation of other message processing</li>
     * </ol>
     * Anything that would violate the above should be wrapped in a {@link Minecraft#execute} call.
     */
    void handle(ProtocolClient client);

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
                final FriendlyByteBuf buf = WorldHost.createByteBuf();
                buf.writeBytes(dis, dis.readInt());
                yield new QueryResponse(friend, WorldHost.parseServerStatus(buf));
            }
            case 9 -> new ProxyC2SPacket(dis.readLong(), dis.readAllBytes());
            case 10 -> new ProxyConnect(dis.readLong(), InetAddress.getByAddress(dis.readNBytes(dis.readUnsignedByte())));
            case 11 -> new ProxyDisconnect(dis.readLong());
            case 12 -> new ConnectionInfo(readUuid(dis), readString(dis), dis.readUnsignedShort());
            default -> new Error("Received packet with unknown type_id from server (outdated client?): " + typeId);
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
}
