package io.github.gaming32.worldhost.common;

import com.mojang.logging.LogUtils;
import io.github.gaming32.worldhost.common.upnp.Gateway;
import io.github.gaming32.worldhost.common.upnp.GatewayFinder;
import io.github.gaming32.worldhost.common.ws.WorldHostWSClient;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;

public class WorldHostCommon {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final File CACHE_DIR = FabricLoader.getInstance()
        .getGameDir()
        .resolve(".world-host-cache")
        .toFile();

    private static Services apiServices;
    public static boolean attemptingConnection;
    public static WorldHostWSClient wsClient;
    private static long lastReconnectTime;

    private static Future<Void> authenticatingFuture;

    public static final Set<UUID> ONLINE_FRIENDS = new HashSet<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = new Long2ObjectOpenHashMap<>();

    public static Gateway upnpGateway;

    private static WorldHostPlatform platform;

    public static void init(WorldHostPlatform platform) {
        if (WorldHostCommon.platform != null) {
            throw new IllegalStateException("Cannot set World Host platform twice!");
        }
        WorldHostCommon.platform = platform;
        init();
    }

    public static WorldHostPlatform getPlatform() {
        if (platform == null) {
            throw new IllegalStateException("Cannot get World Host platform before init()");
        }
        return platform;
    }

    public static Services getApiServices() {
        return apiServices;
    }

    private static void init() {
        apiServices = platform.createServices();

        platform.registerClientTickHandler(client -> {
            if (wsClient == null) {
                authenticatingFuture = null;
                final long time = Util.getMillis();
                if (time - lastReconnectTime > 10_000) {
                    lastReconnectTime = time;
                    if (!attemptingConnection) {
                        reconnect(true, false);
                    }
                }
            }
            if (authenticatingFuture != null && authenticatingFuture.isDone()) {
                authenticatingFuture = null;
                LOGGER.info("Finished authenticating with WS server. Requesting friends list.");
                ONLINE_FRIENDS.clear();
                wsClient.listOnline(WorldHostData.friends);
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null && server.isPublished()) {
                    wsClient.publishedWorld(WorldHostData.friends);
                }
            }
        });

        new GatewayFinder(gateway -> {
            LOGGER.info("Found UPnP gateway: {}", gateway.getGatewayIP());
            upnpGateway = gateway;
        });
    }

    public static void reconnect(boolean successToast, boolean failureToast) {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                LOGGER.error("Failed to close connection to WS server", e);
                if (failureToast) {
                    DeferredToastManager.show(
                        SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE,
                        Components.translatable("world-host.ws_connect.close_failed"),
                        Components.immutable(Util.describeError(e))
                    );
                }
            } finally {
                wsClient = null;
            }
        }
        final UUID uuid = Minecraft.getInstance().getUser().getProfileId();
        if (uuid == null) {
            LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Components.translatable("world-host.ws_connect.not_available"),
                    null
                );
            }
            return;
        }
        attemptingConnection = true;
        // TODO: Do here down as async/figure out why Jetty doesn't work async
        LOGGER.info("Attempting to connect to WS server at {}", WorldHostData.serverUri);
        try {
            wsClient = new WorldHostWSClient(new URI(WorldHostData.serverUri));
        } catch (Exception e) {
            LOGGER.error("Failed to connect to WS server", e);
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.PACK_COPY_FAILURE,
                    Components.translatable("world-host.ws_connect.connect_failed"),
                    Components.immutable(Util.describeError(e))
                );
            }
        }
        attemptingConnection = false;
        if (wsClient != null) {
            authenticatingFuture = wsClient.authenticate(Minecraft.getInstance().getUser().getProfileId());
            if (successToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE,
                    Components.translatable("world-host.ws_connect.connected"),
                    null
                );
            }
        }
    }

    public static void pingFriends() {
        ONLINE_FRIEND_PINGS.clear();
        if (wsClient != null) {
            wsClient.queryRequest(WorldHostData.friends);
        }
    }
}
