package io.github.gaming32.worldhost.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.GeneralUtil;
import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.ws.WorldHostWSClient;
import io.github.gaming32.worldhost.mixin.client.MinecraftAccessor;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;

@Environment(EnvType.CLIENT)
public class WorldHostClient implements ClientModInitializer {
    public static final Services API_SERVICES = Services.create(
        ((MinecraftAccessor)Minecraft.getInstance()).getAuthenticationService(),
        WorldHost.CACHE_DIR
    );
    public static boolean attemptingConnection;
    public static WorldHostWSClient wsClient;
    private static long lastReconnectTime;

    private static Future<Void> authenticatingFuture;

    public static final Set<UUID> ONLINE_FRIENDS = new HashSet<>();
    public static final Map<UUID, ServerStatus> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = new Long2ObjectOpenHashMap<>();

    public static Gateway upnpGateway;

    @Override
    public void onInitializeClient() {
        API_SERVICES.profileCache().setExecutor(Util.backgroundExecutor());
        reconnect(false, true);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
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
                WorldHost.LOGGER.info("Finished authenticating with WS server. Requesting friends list.");
                ONLINE_FRIENDS.clear();
                wsClient.listOnline(WorldHostData.friends);
                final IntegratedServer server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null && server.isPublished()) {
                    wsClient.publishedWorld(WorldHostData.friends);
                }
            }
        });
        new GatewayFinder(gateway -> {
            WorldHost.LOGGER.info("Found UPnP gateway: {}", gateway.getGatewayIP());
            upnpGateway = gateway;
        });
    }

    public static void showProfileToast(UUID user, String title, Component description) {
        Util.backgroundExecutor().execute(() -> {
            final GameProfile profile = Minecraft.getInstance()
                .getMinecraftSessionService()
                .fillProfileProperties(new GameProfile(user, null), false);
            Minecraft.getInstance().execute(() -> {
                final ResourceLocation skinTexture = Minecraft.getInstance().getSkinManager().getInsecureSkinLocation(profile);
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.PERIODIC_NOTIFICATION,
                    (matrices, x, y) -> {
                        RenderSystem.setShaderTexture(0, skinTexture);
                        RenderSystem.enableBlend();
                        GuiComponent.blit(matrices, x, y, 20, 20, 8, 8, 8, 8, 64, 64);
                        GuiComponent.blit(matrices, x, y, 20, 20, 40, 8, 8, 8, 64, 64);
                    },
                    Component.translatable(title, GeneralUtil.getName(profile)),
                    description
                );
            });
        });
    }

    public static void reconnect(boolean successToast, boolean failureToast) {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to close connection to WS server", e);
                if (failureToast) {
                    DeferredToastManager.show(
                        SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE,
                        Component.translatable("world-host.ws_connect.close_failed"),
                        Component.nullToEmpty(Util.describeError(e))
                    );
                }
            } finally {
                wsClient = null;
            }
        }
        final UUID uuid = Minecraft.getInstance().getUser().getProfileId();
        if (uuid == null) {
            WorldHost.LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.TUTORIAL_HINT,
                    Component.translatable("world-host.ws_connect.not_available"),
                    null
                );
            }
            return;
        }
        attemptingConnection = true;
        // TODO: Do here down as async/figure out why Jetty doesn't work async
        WorldHost.LOGGER.info("Attempting to connect to WS server at {}", WorldHostData.serverUri);
        try {
            wsClient = new WorldHostWSClient(new URI(WorldHostData.serverUri));
        } catch (Exception e) {
            WorldHost.LOGGER.error("Failed to connect to WS server", e);
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.PACK_COPY_FAILURE,
                    Component.translatable("world-host.ws_connect.connect_failed"),
                    Component.nullToEmpty(Util.describeError(e))
                );
            }
        }
        attemptingConnection = false;
        if (wsClient != null) {
            authenticatingFuture = wsClient.authenticate(Minecraft.getInstance().getUser().getProfileId());
            if (successToast) {
                DeferredToastManager.show(
                    SystemToast.SystemToastIds.WORLD_ACCESS_FAILURE,
                    Component.translatable("world-host.ws_connect.connected"),
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
