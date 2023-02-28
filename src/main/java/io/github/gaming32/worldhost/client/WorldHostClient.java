package io.github.gaming32.worldhost.client;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.mixin.client.MinecraftClientAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.ApiServices;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class WorldHostClient implements ClientModInitializer {
    public static final ApiServices API_SERVICES = ApiServices.create(
        ((MinecraftClientAccessor)MinecraftClient.getInstance()).getAuthenticationService(),
        WorldHost.CACHE_DIR
    );

    @Override
    public void onInitializeClient() {
        API_SERVICES.userCache().setExecutor(Util.getMainWorkerExecutor());
    }
}
