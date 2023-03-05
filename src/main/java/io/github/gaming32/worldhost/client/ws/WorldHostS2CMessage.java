package io.github.gaming32.worldhost.client.ws;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.FriendsListUpdate;
import io.github.gaming32.worldhost.client.WorldHostClient;
import io.github.gaming32.worldhost.upnp.UPnPErrors;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.query.QueryResponseS2CPacket;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server/blob/main/src/s2c_message.rs
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
                final IntegratedServer server = MinecraftClient.getInstance().getServer();
                if (server != null && server.isRemote()) {
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
            MinecraftClient.getInstance().execute(() -> {
                final MinecraftClient client = MinecraftClient.getInstance();
                ConnectScreen.connect(client.currentScreen, client, new ServerAddress(host, port), null);
            });
        }
    }

    record FriendRequest(UUID fromUser) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            WorldHostClient.showProfileToast(
                fromUser, "world-host.friend_added_you",
                WorldHostData.friends.contains(fromUser) ? null : Text.translatable("world-host.need_add_back")
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
                Text.translatable("world-host.went_online.desc")
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
                final IntegratedServer server = MinecraftClient.getInstance().getServer();
                if (server == null || !server.isRemote()) return;
                if (WorldHostClient.upnpGateway != null) {
                    try {
                        final UPnPErrors.AddPortMappingErrors error = WorldHostClient.upnpGateway.openPort(
                            server.getServerPort(), 60, false
                        );
                        if (error == null) {
                            session.getAsyncRemote().sendObject(new WorldHostC2SMessage.JoinGranted(
                                connectionId, new JoinType.UPnP(server.getServerPort())
                            ));
                            return;
                        }
                        WorldHost.LOGGER.info("Failed to use UPnP mode due to {}. Falling back to Proxy mode.", error);
                    } catch (Exception e) {
                        WorldHost.LOGGER.error("Failed to open UPnP", e);
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
                final IntegratedServer server = MinecraftClient.getInstance().getServer();
                if (server != null) {
                    session.getAsyncRemote().sendObject(new WorldHostC2SMessage.QueryResponse(
                        connectionId, server.getServerMetadata()
                    ));
                }
            }
        }
    }

    record QueryResponse(UUID friend, ServerMetadata metadata) implements WorldHostS2CMessage {
        @Override
        public void handle(Session session) {
            if (WorldHostData.friends.contains(friend)) {
                WorldHostClient.ONLINE_FRIEND_PINGS.put(friend, metadata);
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
                final PacketByteBuf buf = PacketByteBufs.create();
                buf.writeBytes(dis, dis.readInt());
                yield new QueryResponse(
                    friend,
                    new QueryResponseS2CPacket(buf).getServerMetadata()
                );
            }
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
