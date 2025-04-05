package io.github.gaming32.worldhost.protocol;

import com.google.common.net.HostAndPort;
import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.FriendsListUpdate;
import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostUpdateChecker;
import io.github.gaming32.worldhost.gui.screen.AddFriendScreen;
import io.github.gaming32.worldhost.gui.screen.FriendsScreen;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.gui.screen.OnlineFriendsScreen;
import io.github.gaming32.worldhost.plugin.vanilla.WorldHostFriendListFriend;
import io.github.gaming32.worldhost.plugin.vanilla.WorldHostOnlineFriend;
import io.github.gaming32.worldhost.protocol.proxy.ProxyProtocolClient;
import io.github.gaming32.worldhost.protocol.punch.PunchManager;
import io.github.gaming32.worldhost.protocol.punch.PunchReason;
import io.github.gaming32.worldhost.toast.WHToast;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

// Mirrors https://github.com/Gaming32/world-host-server-rust/blob/main/src/protocol/s2c_message.rs
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
        public static final int ID = 0;

        public Error(String message) {
            this(message, false);
        }

        public static Error decode(DataInputStream dis) throws IOException {
            return new Error(readString(dis), dis.read() > 0); // -1 means that there was no critical flag sent
        }

        @Override
        public void handle(ProtocolClient client) {
            if (critical) {
                WHToast.builder("world-host.protocol_error_occurred")
                    .description(Component.literal(message))
                    .show();
                throw new RuntimeException(message);
            } else {
                WorldHost.LOGGER.error("Received protocol error: {}", message);
            }
        }
    }

    record IsOnlineTo(UUID user) implements WorldHostS2CMessage {
        public static final int ID = 1;

        public static IsOnlineTo decode(DataInputStream dis) throws IOException {
            return new IsOnlineTo(readUuid(dis));
        }

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
        public static final int ID = 2;

        public static OnlineGame decode(DataInputStream dis) throws IOException {
            return new OnlineGame(readString(dis), dis.readUnsignedShort(), dis.readLong(), dis.readBoolean());
        }

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
        public static final int ID = 3;

        public static FriendRequest decode(DataInputStream dis) throws IOException {
            return new FriendRequest(readUuid(dis), SecurityLevel.byId(dis.readUnsignedByte()));
        }

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
                WorldHost.resolveProfileInfo(new GameProfile(fromUser, "")),
                isFriend ? "world-host.friend_added_you.already" : "world-host.friend_added_you",
                isFriend ? "world-host.friend_added_you.already.desc" : "world-host.need_add_back",
                isFriend ? 100 : 200,
                isFriend ? null : () -> {
                    final Minecraft minecraft = Minecraft.getInstance();
                    minecraft.setScreen(new AddFriendScreen(
                        minecraft.screen,
                        FriendsScreen.ADD_FRIEND_TEXT,
                        new WorldHostFriendListFriend(fromUser),
                        (friend, notify) -> friend.addFriend(notify, () -> {})
                    ));
                }
            );
        }
    }

    record PublishedWorld(
        UUID user, long connectionId, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        public static final int ID = 4;

        public static PublishedWorld decode(DataInputStream dis) throws IOException {
            return new PublishedWorld(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
        }

        @Override
        public void handle(ProtocolClient client) {
            if (!checkAndLogSecurity() || !WorldHost.isFriend(user)) return;
            Minecraft.getInstance().execute(() ->
                WorldHost.friendWentOnline(new WorldHostOnlineFriend(user, connectionId, security))
            );
        }
    }

    record ClosedWorld(UUID user) implements WorldHostS2CMessage {
        public static final int ID = 5;

        public static ClosedWorld decode(DataInputStream dis) throws IOException {
            return new ClosedWorld(readUuid(dis));
        }

        @Override
        public void handle(ProtocolClient client) {
            WorldHost.ONLINE_FRIENDS.remove(user);
            WorldHost.ONLINE_FRIEND_PINGS.remove(user);
            WorldHost.ONLINE_FRIEND_UPDATES.forEach(FriendsListUpdate::friendsListUpdate);
        }
    }

    record RequestJoin(
        UUID user, long connectionId, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        public static final int ID = 6;

        public static RequestJoin decode(DataInputStream dis) throws IOException {
            return new RequestJoin(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
        }

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
        public static final int ID = 7;

        public static QueryRequest decode(DataInputStream dis) throws IOException {
            return new QueryRequest(readUuid(dis), dis.readLong(), SecurityLevel.byId(dis.readUnsignedByte()));
        }

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
        public static final int ID = 8;

        public static QueryResponse decode(DataInputStream dis) throws IOException {
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
            return new QueryResponse(friend, serverStatus);
        }

        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(friend)) {
                WorldHost.ONLINE_FRIEND_PINGS.put(friend, metadata);
            }
        }
    }

    record ProxyC2SPacket(long connectionId, byte[] data) implements WorldHostS2CMessage {
        public static final int ID = 9;

        public static ProxyC2SPacket decode(DataInputStream dis) throws IOException {
            return new ProxyC2SPacket(dis.readLong(), dis.readAllBytes());
        }

        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyPacket(connectionId, data);
        }
    }

    record ProxyConnect(long connectionId, InetAddress remoteAddr) implements WorldHostS2CMessage {
        public static final int ID = 10;

        public static ProxyConnect decode(DataInputStream dis) throws IOException {
            return new ProxyConnect(dis.readLong(), InetAddress.getByAddress(dis.readNBytes(dis.readUnsignedByte())));
        }

        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyConnect(connectionId, remoteAddr, () -> WorldHost.protoClient);
        }
    }

    record ProxyDisconnect(long connectionId) implements WorldHostS2CMessage {
        public static final int ID = 11;

        public static ProxyDisconnect decode(DataInputStream dis) throws IOException {
            return new ProxyDisconnect(dis.readLong());
        }

        @Override
        public void handle(ProtocolClient client) {
            WorldHost.proxyDisconnect(connectionId);
        }
    }

    record ConnectionInfo(
        long connectionId, String baseIp, int basePort, String userIp, int protocolVersion, int punchPort
    ) implements WorldHostS2CMessage {
        public static final int ID = 12;

        public static ConnectionInfo decode(DataInputStream dis) throws IOException {
            return new ConnectionInfo(
                dis.readLong(), readString(dis), dis.readUnsignedShort(), readString(dis), dis.readInt(), dis.readUnsignedShort()
            );
        }

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
        public static final int ID = 13;

        public static ExternalProxyServer decode(DataInputStream dis) throws IOException {
            return new ExternalProxyServer(
                readString(dis), dis.readUnsignedShort(), readString(dis), dis.readUnsignedShort()
            );
        }

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
        public static final int ID = 14;

        public static OutdatedWorldHost decode(DataInputStream dis) throws IOException {
            return new OutdatedWorldHost(readString(dis));
        }

        @Override
        public void handle(ProtocolClient client) {
            final String currentVersion = WorldHost.getModVersion(WorldHost.MOD_ID);
            WorldHost.LOGGER.info(I18n.get(
                "world-host.outdated_world_host.desc",
                currentVersion, recommendedVersion + '+'
            ));
            if (!WorldHost.CONFIG.isShowOutdatedWorldHost()) return;
            WorldHostUpdateChecker.checkForUpdates().thenAcceptAsync(version -> {
                if (version.isEmpty()) return;
                final String updateLink = WorldHostUpdateChecker.formatUpdateLink(version.get());
                WHToast.builder("world-host.outdated_world_host")
                    .description(Component.translatable(
                        "world-host.outdated_world_host.desc",
                        currentVersion, version.get()
                    ))
                    .clickAction(() -> Util.getPlatform().openUri(updateLink))
                    .ticks(200)
                    .show();
            }, Minecraft.getInstance());
        }
    }

    record ConnectionNotFound(long connectionId) implements WorldHostS2CMessage {
        public static final int ID = 15;

        public static ConnectionNotFound decode(DataInputStream dis) throws IOException {
            return new ConnectionNotFound(dis.readLong());
        }

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
                    Component.translatable("world-host.connection_not_found"),
                    Component.translatable("world-host.connection_not_found.desc", WorldHost.connectionIdToString(connectionId))
                ));
            });
        }
    }

    record NewQueryResponse(UUID friend, ServerStatus metadata) implements WorldHostS2CMessage {
        public static final int ID = 16;

        public static NewQueryResponse decode(DataInputStream dis) throws IOException {
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
            return new NewQueryResponse(friend, serverStatus);
        }

        @Override
        public void handle(ProtocolClient client) {
            if (WorldHost.isFriend(friend)) {
                WorldHost.ONLINE_FRIEND_PINGS.put(friend, metadata);
            }
        }
    }

    record Warning(String message, boolean important) implements WorldHostS2CMessage {
        public static final int ID = 17;

        public static Warning decode(DataInputStream dis) throws IOException {
            return new Warning(readString(dis), dis.readBoolean());
        }

        @Override
        public void handle(ProtocolClient client) {
            WorldHost.LOGGER.warn("Warning from WH server (important: {}): {}", important, message);
            WHToast.builder(Component.translatable("world-host.protocol_warning_occurred"))
                .description(Component.literal(message))
                .important(important)
                .show();
        }
    }

    record PunchOpenRequest(
        UUID punchId, String purpose, String fromHost, int fromPort, long connectionId, UUID user, SecurityLevel security
    ) implements WorldHostS2CMessage, SecurityCheckable {
        public static final int ID = 18;

        public static PunchOpenRequest decode(DataInputStream dis) throws IOException {
            return new PunchOpenRequest(
                readUuid(dis),
                readString(dis),
                readString(dis),
                dis.readUnsignedShort(),
                dis.readLong(),
                readUuid(dis),
                SecurityLevel.byId(dis.readUnsignedByte())
            );
        }

        @Override
        public void handle(ProtocolClient client) {
            if (!checkAndLogSecurity()) return;
            final PunchReason reason = PunchReason.byId(purpose);
            if (reason == null) {
                WorldHost.LOGGER.warn("Punch {} from {} has unknown purpose {}", punchId, user, purpose);
                client.punchFailed(connectionId, punchId);
                return;
            }
            if (!reason.verificationType().verify(user)) {
                WorldHost.LOGGER.warn(
                    "Punch {} from {} failed verification (verification type {})",
                    punchId, user, PunchReason.VerificationType.getName(reason.verificationType())
                );
                client.punchFailed(connectionId, punchId);
                return;
            }
            final var transmitter = reason.transmitterFinder().findServerTransmitter();
            if (transmitter == null) {
                WorldHost.LOGGER.warn(
                    "Punch {} from {} couldn't find transmitter (transmitter finder {})",
                    punchId, user, reason.transmitterFinder()
                );
                client.punchFailed(connectionId, punchId);
                return;
            }
            Minecraft.getInstance().execute(
                () -> PunchManager.openPunchRequest(punchId, transmitter, fromHost, fromPort, connectionId)
            );
        }
    }

    record CancelPortLookup(UUID lookupId) implements WorldHostS2CMessage {
        public static final int ID = 19;

        public static CancelPortLookup decode(DataInputStream dis) throws IOException {
            return new CancelPortLookup(readUuid(dis));
        }

        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> PunchManager.cancelPortLookup(lookupId));
        }
    }

    record PortLookupSuccess(UUID lookupId, String host, int port) implements WorldHostS2CMessage {
        public static final int ID = 20;

        public static PortLookupSuccess decode(DataInputStream dis) throws IOException {
            var lookupId = readUuid(dis);
            return new PortLookupSuccess(lookupId, readString(dis), dis.readUnsignedShort());
        }

        @Override
        public void handle(ProtocolClient client) {
            final HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
            Minecraft.getInstance().execute(() -> PunchManager.portLookupSuccess(lookupId, hostAndPort));
        }
    }

    record PunchRequestCancelled(UUID punchId) implements WorldHostS2CMessage {
        public static final int ID = 21;

        public static PunchRequestCancelled decode(DataInputStream dis) throws IOException {
            return new PunchRequestCancelled(readUuid(dis));
        }

        @Override
        public void handle(ProtocolClient client) {
            Minecraft.getInstance().execute(() -> PunchManager.cancelPunch(punchId));
        }
    }

    record PunchSuccess(UUID punchId, String host, int port) implements WorldHostS2CMessage {
        public static final int ID = 22;

        public static PunchSuccess decode(DataInputStream dis) throws IOException {
            return new PunchSuccess(readUuid(dis), readString(dis), dis.readUnsignedShort());
        }

        @Override
        public void handle(ProtocolClient client) {
            final HostAndPort hostAndPort = HostAndPort.fromParts(host, port);
            Minecraft.getInstance().execute(() -> PunchManager.punchSuccess(punchId, hostAndPort));
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

    static WorldHostS2CMessage decode(int typeId, DataInputStream dis) throws IOException {
        return switch (typeId) {
            case Error.ID -> Error.decode(dis);
            case IsOnlineTo.ID -> IsOnlineTo.decode(dis);
            case OnlineGame.ID -> OnlineGame.decode(dis);
            case FriendRequest.ID -> FriendRequest.decode(dis);
            case PublishedWorld.ID -> PublishedWorld.decode(dis);
            case ClosedWorld.ID -> ClosedWorld.decode(dis);
            case RequestJoin.ID -> RequestJoin.decode(dis);
            case QueryRequest.ID -> QueryRequest.decode(dis);
            case QueryResponse.ID -> QueryResponse.decode(dis);
            case ProxyC2SPacket.ID -> ProxyC2SPacket.decode(dis);
            case ProxyConnect.ID -> ProxyConnect.decode(dis);
            case ProxyDisconnect.ID -> ProxyDisconnect.decode(dis);
            case ConnectionInfo.ID -> ConnectionInfo.decode(dis);
            case ExternalProxyServer.ID -> ExternalProxyServer.decode(dis);
            case OutdatedWorldHost.ID -> OutdatedWorldHost.decode(dis);
            case ConnectionNotFound.ID -> ConnectionNotFound.decode(dis);
            case NewQueryResponse.ID -> NewQueryResponse.decode(dis);
            case Warning.ID -> Warning.decode(dis);
            case PunchOpenRequest.ID -> PunchOpenRequest.decode(dis);
            case CancelPortLookup.ID -> CancelPortLookup.decode(dis);
            case PortLookupSuccess.ID -> PortLookupSuccess.decode(dis);
            case PunchRequestCancelled.ID -> PunchRequestCancelled.decode(dis);
            case PunchSuccess.ID -> PunchSuccess.decode(dis);
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
