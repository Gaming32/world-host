package io.github.gaming32.worldhost.client;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.ws.WorldHostWSClient;
import io.github.gaming32.worldhost.mixin.client.MinecraftClientAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class WorldHostClient implements ClientModInitializer {
    public static final ApiServices API_SERVICES = ApiServices.create(
        ((MinecraftClientAccessor)MinecraftClient.getInstance()).getAuthenticationService(),
        WorldHost.CACHE_DIR
    );
    public static WorldHostWSClient wsClient;

    @Override
    public void onInitializeClient() {
        API_SERVICES.userCache().setExecutor(Util.getMainWorkerExecutor());
        reconnect();
    }

    public static void reconnect() {
        if (wsClient != null) {
            try {
                wsClient.close();
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to close connection to WS server", e);
            } finally {
                wsClient = null;
            }
        }
        final UUID uuid = MinecraftClient.getInstance().getSession().getUuidOrNull();
        if (uuid == null) {
            WorldHost.LOGGER.warn("Failed to get player UUID. Unable to use World Host.");
            return;
        }
        WorldHost.LOGGER.info("Attempting to connect to WS server at {}", WorldHostData.serverUri);
        try {
            wsClient = new WorldHostWSClient(new URI(WorldHostData.serverUri));
        } catch (Exception e) {
            WorldHost.LOGGER.error("Failed to connect to WS server", e);
        }
        if (wsClient != null) {
            try {
                wsClient.authenticate(MinecraftClient.getInstance().getSession().getUuidOrNull());
            } catch (Exception e) {
                WorldHost.LOGGER.error("Failed to connect to WS server", e);
            }
        }
    }
}
