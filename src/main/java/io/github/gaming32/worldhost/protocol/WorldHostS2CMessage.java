package io.github.gaming32.worldhost.protocol;

import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.upnp.UPnPErrors;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

//#if MC > 11605
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//#endif

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostS2CMessage.kt
public sealed interface WorldHostS2CMessage {
    record Error(String message) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.error("Received protocol error: {}", message);
//            WHToast.builder("world-host.protocol_error_occurred")
//                .description(Components.immutable(message))
//                .show();
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

    record OnlineGame(String host, int port, long ownerCid) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> {
                final Long attemptingToJoin = client.getAttemptingToJoin();
                if (attemptingToJoin == null || ownerCid != attemptingToJoin) return;
                final Minecraft minecraft = Minecraft.getInstance();
                assert minecraft.screen != null;
                Screen parentScreen = minecraft.screen;
                if (parentScreen instanceof JoiningWorldHostScreen joinScreen) {
                    parentScreen = joinScreen.parent;
                }
                //#if MC > 11605
                //noinspection DataFlowIssue // IntelliJ, it's literally marked @Nullable :clown:
                ConnectScreen.startConnecting(parentScreen, minecraft, new ServerAddress(host, port), null);
                //#else
                //$$ minecraft.setCurrentServer(null);
                //$$ minecraft.setScreen(new ConnectScreen(parentScreen, minecraft, host, port));
                //#endif
            });
        }
    }

    record FriendRequest(UUID fromUser) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (!WorldHost.CONFIG.isEnableFriends()) return;
            final boolean isFriend = WorldHost.isFriend(fromUser);
            WorldHost.showFriendOrOnlineToast(
                fromUser,
                isFriend ? "world-host.friend_added_you.already" : "world-host.friend_added_you",
                isFriend ? "world-host.friend_added_you.already.desc" : "world-host.need_add_back",
                isFriend ? 100 : 200,
                isFriend ? null : () -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new AddFriendScreen(
                        minecraft.screen,
                        FriendsScreen.ADD_FRIEND_TEXT,
                        fromUser,
                        FriendsScreen::addFriend
                    ));
                }
            );
        }
    }

    record PublishedWorld(UUID user, long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (!WorldHost.isFriend(user)) return;
            Minecraft.getInstance().execute(() -> {
                WorldHost.ONLINE_FRIENDS.put(user, connectionId);
                WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
                WorldHost.showFriendOrOnlineToast(
                    user, "world-host.went_online", "world-host.went_online.desc", 200,
                    () -> WorldHost.join(connectionId, null)
                );
            });
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

    record RequestJoin(UUID user, long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(user)) {
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server == null || !server.isPublished()) return;
                if (WorldHost.upnpGateway != null && !WorldHost.CONFIG.isNoUPnP()) {
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

    record QueryRequest(UUID friend, long connectionId) implements WorldHostS2CMessage {
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

    record ProxyC2SPacket(long connectionId, byte[] data) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyPacket(connectionId, data);
        }
    }

    record ProxyConnect(long connectionId, InetAddress remoteAddr) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyConnect(connectionId, remoteAddr, () -> WorldHost.protoClient);
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyDisconnect(connectionId);
        }
    }

    record ConnectionInfo(
        long connectionId, String baseIp, int basePort, String userIp, int protocolVersion
    ) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            client.setConnectionId(connectionId);
            client.setBaseIp(baseIp);
            client.setBasePort(basePort);
            client.setUserIp(userIp);
            if (ProtocolClient.PROTOCOL_VERSION < protocolVersion) {
                WorldHost.LOGGER.warn(
                    "Client is out of date with server! Client version: {}. Server version: {}.",
                    ProtocolClient.PROTOCOL_VERSION, protocolVersion
                );
            }
        }
    }

    record ExternalProxyServer(String host, int port, String baseAddr, int mcPort) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.info("Attempting to connect to WHEP server at {}, {}", host, port);
            if (WorldHost.proxyProtocolClient != null) {
                WorldHost.proxyProtocolClient.close(); // Shouldn't happen, but better safe than sorry
            }
            WorldHost.proxyProtocolClient = new ProxyProtocolClient(host, port, client.getConnectionId(), baseAddr, mcPort);
        }
    }

    record OutdatedWorldHost(String recommendedVersion) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            final String currentVersion = WorldHost.getModVersion(WorldHost.MOD_ID);
            WorldHost.LOGGER.info(I18n.get("world-host.outdated_world_host.desc", currentVersion, recommendedVersion));
            if (!WorldHost.CONFIG.isShowOutdatedWorldHost()) return;
            WHToast.builder("world-host.outdated_world_host")
                .description(Components.translatable("world-host.outdated_world_host.desc", currentVersion, recommendedVersion))
                .clickAction(() -> Util.getPlatform().openUri(
                    "https://modrinth.com/mod/world-host/version/" +
                        recommendedVersion + '+' + WorldHost.getModVersion("minecraft") + '-' + WorldHost.MOD_LOADER
                ))
                .ticks(200)
                .show();
        }
    }

    record ConnectionNotFound(long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> {
                if (client.getAttemptingToJoin() == null || client.getAttemptingToJoin() != connectionId) return;
                client.setAttemptingToJoin(null);
                final Minecraft minecraft = Minecraft.getInstance();
                Screen parentScreen = minecraft.screen;
                if (parentScreen instanceof JoiningWorldHostScreen joinScreen) {
                    parentScreen = joinScreen.parent;
                }
                //noinspection DataFlowIssue // Why do I care if parentScreen is null?
                minecraft.setScreen(new DisconnectedScreen(
                    parentScreen,
                    //#if MC > 11601
                    Components.translatable("world-host.connection_not_found"),
                    //#else
                    //$$ "world-host.connection_not_found",
                    //#endif
                    Components.translatable("world-host.connection_not_found.desc", WorldHost.connectionIdToString(connectionId))
                ));
            });
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
            case 2 -> new OnlineGame(readString(dis), dis.readUnsignedShort(), dis.readLong());
            case 3 -> new FriendRequest(readUuid(dis));
            case 4 -> new PublishedWorld(readUuid(dis), dis.readLong());
            case 5 -> new ClosedWorld(readUuid(dis));
            case 6 -> new RequestJoin(readUuid(dis), dis.readLong());
            case 7 -> new QueryRequest(readUuid(dis), dis.readLong());
            case 8 -> {
                final UUID friend = readUuid(dis);
                final FriendlyByteBuf buf = WorldHost.createByteBuf();
                buf.writeBytes(dis, dis.readInt());
                ServerStatus serverStatus;
                try {
                    serverStatus = WorldHost.parseServerStatus(buf);
                } catch (Exception e) {
                    WorldHost.LOGGER.error("Failed to parse server status", e);
                    serverStatus = WorldHost.createEmptyServerStatus();
                }
                yield new QueryResponse(friend, serverStatus);
            }
            case 9 -> new ProxyC2SPacket(dis.readLong(), dis.readAllBytes());
            case 10 -> new ProxyConnect(dis.readLong(), InetAddress.getByAddress(dis.readNBytes(dis.readUnsignedByte())));
            case 11 -> new ProxyDisconnect(dis.readLong());
            case 12 -> new ConnectionInfo(
                dis.readLong(), readString(dis), dis.readUnsignedShort(), readString(dis), dis.readInt()
            );
            case 13 -> new ExternalProxyServer(
                readString(dis), dis.readUnsignedShort(), readString(dis), dis.readUnsignedShort()
            );
            case 14 -> new OutdatedWorldHost(readString(dis));
            case 15 -> new ConnectionNotFound(dis.readLong());
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
