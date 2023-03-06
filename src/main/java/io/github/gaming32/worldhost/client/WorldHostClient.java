package io.github.gaming32.worldhost.client;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.GeneralUtil;
import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.ws.WorldHostWSClient;
import io.github.gaming32.worldhost.mixin.client.MinecraftClientAccessor;
import io.github.gaming32.worldhost.upnp.Gateway;
import io.github.gaming32.worldhost.upnp.GatewayFinder;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.server.ServerMetadata;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.*;
import java.util.concurrent.Future;

@Environment(EnvType.CLIENT)
public class WorldHostClient implements ClientModInitializer {
    public static final ApiServices API_SERVICES = ApiServices.create(
        ((MinecraftClientAccessor)MinecraftClient.getInstance()).getAuthenticationService(),
        WorldHost.CACHE_DIR
    );
    public static WorldHostWSClient wsClient;
    private static long lastReconnectTime;

    private static Future<Void> authenticatingFuture;

    public static final Set<UUID> ONLINE_FRIENDS = new HashSet<>();
    public static final Map<UUID, ServerMetadata> ONLINE_FRIEND_PINGS = new HashMap<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    public static final Long2ObjectMap<ProxyClient> CONNECTED_PROXY_CLIENTS = new Long2ObjectOpenHashMap<>();

    public static Gateway upnpGateway;

    @Override
    public void onInitializeClient() {
        API_SERVICES.userCache().setExecutor(Util.getMainWorkerExecutor());
        reconnect(false, true);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (wsClient == null) {
                authenticatingFuture = null;
                final long time = Util.getMeasuringTimeMs();
                if (time - lastReconnectTime > 10_000) {
                    lastReconnectTime = time;
                    reconnect(true, false);
                }
            }
            if (authenticatingFuture != null && authenticatingFuture.isDone()) {
                authenticatingFuture = null;
                WorldHost.LOGGER.info("Finished authenticating with WS server. Requesting friends list.");
                ONLINE_FRIENDS.clear();
                wsClient.listOnline(WorldHostData.friends);
                final IntegratedServer server = MinecraftClient.getInstance().getServer();
                if (server != null && server.isRemote()) {
                    wsClient.publishedWorld(WorldHostData.friends);
                }
            }
        });
        new GatewayFinder(gateway -> {
            WorldHost.LOGGER.info("Found UPnP gateway: {}", gateway.getGatewayIP());
            upnpGateway = gateway;
        });
    }

    public static void showProfileToast(UUID user, String title, Text description) {
        Util.getMainWorkerExecutor().execute(() -> {
            final GameProfile profile = MinecraftClient.getInstance()
                .getSessionService()
                .fillProfileProperties(new GameProfile(user, null), false);
            MinecraftClient.getInstance().execute(() -> {
                final Identifier skinTexture = MinecraftClient.getInstance().getSkinProvider().loadSkin(profile);
                DeferredToastManager.show(
                    SystemToast.Type.PERIODIC_NOTIFICATION,
                    (matrices, x, y) -> {
                        RenderSystem.setShaderTexture(0, skinTexture);
                        RenderSystem.enableBlend();
                        DrawableHelper.drawTexture(matrices, x, y, 20, 20, 8, 8, 8, 8, 64, 64);
                        DrawableHelper.drawTexture(matrices, x, y, 20, 20, 40, 8, 8, 8, 64, 64);
                    },
                    Text.translatable(title, GeneralUtil.getName(profile)),
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
                        SystemToast.Type.WORLD_ACCESS_FAILURE,
                        Text.translatable("world-host.ws_connect.close_failed"),
                        Text.of(Util.getInnermostMessage(e))
                    );
                }
            } finally {
                wsClient = null;
            }
        }
        final UUID uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        if (uuid == null) {
            WorldHost.LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.Type.TUTORIAL_HINT,
                    Text.translatable("world-host.ws_connect.not_available"),
                    null
                );
            }
            return;
        }
        WorldHost.LOGGER.info("Attempting to connect to WS server at {}", WorldHostData.serverUri);
        try {
            wsClient = new WorldHostWSClient(new URI(WorldHostData.serverUri));
        } catch (Exception e) {
            WorldHost.LOGGER.error("Failed to connect to WS server", e);
            if (failureToast) {
                DeferredToastManager.show(
                    SystemToast.Type.PACK_COPY_FAILURE,
                    Text.translatable("world-host.ws_connect.connect_failed"),
                    Text.of(Util.getInnermostMessage(e))
                );
            }
        }
        if (wsClient != null) {
            authenticatingFuture = wsClient.authenticate(MinecraftClient.getInstance().getSession().getUuidOrNull());
            if (successToast) {
                DeferredToastManager.show(
                    SystemToast.Type.WORLD_ACCESS_FAILURE,
                    Text.translatable("world-host.ws_connect.connected"),
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
