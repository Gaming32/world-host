package io.github.gaming32.worldhost.client;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.ws.WorldHostWSClient;
import io.github.gaming32.worldhost.mixin.client.MinecraftClientAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.ApiServices;
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

    private static Future<Void> authenticatingFuture;

    public static final Set<UUID> ONLINE_FRIENDS = new HashSet<>();
    public static final Set<FriendsListUpdate> ONLINE_FRIEND_UPDATES = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public void onInitializeClient() {
        API_SERVICES.userCache().setExecutor(Util.getMainWorkerExecutor());
        reconnect(false);
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (authenticatingFuture != null && authenticatingFuture.isDone()) {
                authenticatingFuture = null;
                WorldHost.LOGGER.info("Finished authenticating with WS server. Requesting friends list.");
                ONLINE_FRIENDS.clear();
                wsClient.requestOnlineFriends(WorldHostData.friends);
            }
        });
    }

    public static void reconnect(boolean successToast) {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to close connection to WS server", e);
                DeferredToastManager.show(
                    SystemToast.Type.WORLD_ACCESS_FAILURE,
                    Text.translatable("world-host.ws_connect.close_failed"),
                    Text.of(e.getLocalizedMessage())
                );
            } finally {
                wsClient = null;
            }
        }
        final UUID uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        if (uuid == null) {
            WorldHost.LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            DeferredToastManager.show(
                SystemToast.Type.TUTORIAL_HINT,
                Text.translatable("world-host.ws_connect.not_available"),
                null
            );
            return;
        }
        WorldHost.LOGGER.info("Attempting to connect to WS server at {}", WorldHostData.serverUri);
        try {
            wsClient = new WorldHostWSClient(new URI(WorldHostData.serverUri));
        } catch (Exception e) {
            WorldHost.LOGGER.error("Failed to connect to WS server", e);
            DeferredToastManager.show(
                SystemToast.Type.PACK_COPY_FAILURE,
                Text.translatable("world-host.ws_connect.connect_failed"),
                Text.of(e.getLocalizedMessage())
            );
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
}
