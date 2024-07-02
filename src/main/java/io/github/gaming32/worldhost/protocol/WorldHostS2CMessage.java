package io.github.gaming32.worldhost.protocol;

import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostUpdateChecker;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.gui.screen.OnlineFriendsScreen;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.toast.WHToast;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-kotlin/blob/main/src/main/kotlin/io/github/gaming32/worldhostserver/WorldHostS2CMessage.kt
public sealed interface WorldHostS2CMessage {
    interface SecurityCheckable {
        SecurityLevel security();

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        default boolean checkAndLogSecurity() {
            final SecurityLevel receivedLevel = security();
            final SecurityLevel requiredLevel = WorldHost.CONFIG.getRequiredSecurityLevel();
            if (receivedLevel.compareTo(requiredLevel) >= 0) {
                return true;
            }
            WorldHost.LOGGER.warn(
                "Received {} from insecure client. Security is {}, but {} is required.",
                this, receivedLevel, requiredLevel
            );
            return false;
        }
    }

    record Error(String message, boolean critical) implements WorldHostS2CMessage {
        public Error(String message) {
            this(message, false);
        }

        @Override
        public void handle(ProtocolClient client) {
            if (critical) {
                WHToast.builder("world-host.protocol_error_occurred")
                    .description(Components.literal(message))
                    .show();
                throw new RuntimeException(message);
            } else {
                WorldHost.LOGGER.error("Received protocol error: {}", message);
            }
        }
    }

    record IsOnlineTo(UUID user) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(user)) {
                final var server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null && server.isPublished()) {
                    client.publishedWorld(List.of(user));
                }
            }
        }
    }

    record OnlineGame(String host, int port, long ownerCid, boolean isPunchProtocol) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> {
                final Long attemptingToJoin = client.getAttemptingToJoin();
                if (attemptingToJoin == null || ownerCid != attemptingToJoin) return;
                final Minecraft minecraft = Minecraft.getInstance();
                assert minecraft.screen != null;
                if (isPunchProtocol) {
                    WorldHost.LOGGER.error("Punch is not supported!");
                } else {
                    WorldHost.connect(new OnlineFriendsScreen(new TitleScreen()), ownerCid, host, port);
                }
            });
        }
    }

    record FriendRequest(UUID fromUser, SecurityLevel security) implements WorldHostS2CMessage, SecurityCheckable {
        @Override
        public void handle(ProtocolClient client) {
            if (!WorldHost.CONFIG.isEnableFriends() || !checkAndLogSecurity()) return;
            final boolean isFriend = WorldHost.isFriend(fromUser);
            if (isFriend) {
                Minecraft.getInstance().execute(() -> {
                    final var server = Minecraft.getInstance().getSingleplayerServer();
                    if (server != null && server.isPublished()) {
                        client.publishedWorld(Collections.singleton(fromUser));
                    }
                });
            }
            if (!isFriend && !WorldHost.CONFIG.isAllowFriendRequests()) return;
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

    record PublishedWorld(
        UUID user, long connectionId, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        @Override
        public void handle(ProtocolClient client) {
            if (!checkAndLogSecurity() || !WorldHost.isFriend(user)) return;
            Minecraft.getInstance().execute(() -> {
                WorldHost.ONLINE_FRIENDS.put(user, connectionId);
                WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
                if (!WorldHost.CONFIG.isAnnounceFriendsOnline()) return;
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
            WorldHost.ONLINE_FRIENDS.removeLong(user);
            WorldHost.ONLINE_FRIEND_PINGS.remove(user);
            WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        }
    }

    record RequestJoin(
        UUID user, long connectionId, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        @Override
        public void handle(ProtocolClient client) {
            if (!checkAndLogSecurity()) return;
            final var server = Minecraft.getInstance().getSingleplayerServer();
            if (server == null || !server.isPublished()) return;
            JoinType joinType = JoinType.Proxy.INSTANCE;
            if (WorldHost.isFriend(user) && WorldHost.upnpGateway != null && WorldHost.CONFIG.isUPnP()) {
                try {
                    final var error = WorldHost.upnpGateway.openPort(
                        server.getPort(), 60, false
                    );
                    if (error == null) {
                        joinType = new JoinType.UPnP(server.getPort());
                    } else {
                        WorldHost.LOGGER.info("Failed to use UPnP mode due to {}. Falling back to Proxy mode.", error);
                    }
                } catch (Exception e) {
                    WorldHost.LOGGER.error("Failed to open UPnP due to exception", e);
                }
            }
            client.enqueue(new WorldHostC2SMessage.JoinGranted(connectionId, joinType));
        }
    }

    record QueryRequest(
        UUID friend, long connectionId, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        @Override
        public void handle(ProtocolClient client) {
            if (!checkAndLogSecurity() || !WorldHost.isFriend(friend)) return;
            final var server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null) {
                client.enqueue(new WorldHostC2SMessage.NewQueryResponse(connectionId, server.getStatus()));
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
        long connectionId, String baseIp, int basePort, String userIp, int protocolVersion, int punchPort
    ) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.info("Received {}", this);
            client.connectingFuture.complete(null);
            client.setConnectionId(connectionId);
            client.setBaseIp(baseIp);
            client.setBasePort(basePort);
            client.setUserIp(userIp);
            client.setPunchPort(punchPort);
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
            final var message = Components.translatable("world-host.outdated_world_host.desc", currentVersion, recommendedVersion);
            WorldHost.LOGGER.info(message.getString());
            if (!WorldHost.CONFIG.isShowOutdatedWorldHost()) return;
            WorldHostUpdateChecker.checkForUpdates().thenAcceptAsync(version -> {
                if (version.isEmpty()) return;
                final String updateLink = WorldHostUpdateChecker.formatUpdateLink(version.get());
                WHToast.builder("world-host.outdated_world_host")
                    .description(message)
                    .clickAction(() -> Util.getPlatform().openUri(updateLink))
                    .ticks(200)
                    .show();
            }, Minecraft.getInstance());
        }
    }

    record ConnectionNotFound(long connectionId) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> {
                if (client.getAttemptingToJoin() == null || client.getAttemptingToJoin() != connectionId) return;
                client.setAttemptingToJoin(null);
                final Minecraft minecraft = Minecraft.getInstance();
                var parentScreen = minecraft.screen;
                if (parentScreen instanceof JoiningWorldHostScreen joinScreen) {
                    parentScreen = joinScreen.parent;
                }
                minecraft.setScreen(new DisconnectedScreen(
                    parentScreen,
                    Components.translatable("world-host.connection_not_found"),
                    Components.translatable("world-host.connection_not_found.desc", WorldHost.connectionIdToString(connectionId))
                ));
            });
        }
    }

    record NewQueryResponse(UUID friend, ServerStatus metadata) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(friend)) {
                WorldHost.ONLINE_FRIEND_PINGS.put(friend, metadata);
            }
        }
    }

    record Warning(String message, boolean important) implements WorldHostS2CMessage {
        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.warn("Warning from WH server (important: {}): {}", important, message);
            WHToast.builder(Components.translatable("world-host.protocol_warning_occurred"))
                .description(Components.literal(message))
                .important(important)
                .show();
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
            case 0 -> new Error(readString(dis), dis.read() > 0); // -1 means that there was no critical flag sent
            case 1 -> new IsOnlineTo(readUuid(dis));
            case 2 -> new OnlineGame(readString(dis), dis.readUnsignedShort(), dis.readLong(), dis.readBoolean());
            case 3 -> new FriendRequest(readUuid(dis), SecurityLevel.byId(dis.readUnsignedByte()));
            case 4 -> new PublishedWorld(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
            case 5 -> new ClosedWorld(readUuid(dis));
            case 6 -> new RequestJoin(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
            case 7 -> new QueryRequest(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
            case 8 -> {
                final UUID friend = readUuid(dis);
                final var buf = WorldHost.createByteBuf();
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
                dis.readLong(), readString(dis), dis.readUnsignedShort(), readString(dis), dis.readInt(), dis.readUnsignedShort()
            );
            case 13 -> new ExternalProxyServer(
                readString(dis), dis.readUnsignedShort(), readString(dis), dis.readUnsignedShort()
            );
            case 14 -> new OutdatedWorldHost(readString(dis));
            case 15 -> new ConnectionNotFound(dis.readLong());
            case 16 -> {
                final UUID friend = readUuid(dis);
                final var buf = WorldHost.createByteBuf();
                buf.writeBytes(dis.readAllBytes());
                ServerStatus serverStatus;
                try {
                    serverStatus = WorldHost.parseServerStatus(buf);
                } catch (Exception e) {
                    WorldHost.LOGGER.error("Failed to parse server status", e);
                    serverStatus = WorldHost.createEmptyServerStatus();
                }
                yield new NewQueryResponse(friend, serverStatus);
            }
            case 17 -> new Warning(readString(dis), dis.readBoolean());
            default -> new Error("Received packet with unknown typeId from server (outdated client?): " + typeId);
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
