package io.github.gaming32.worldhost.mixin.client;

import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer {
    @Shadow public abstract boolean isPublished();

    @Inject(method = "publishServer", at = @At(value = "RETURN", ordinal = 0))
    private void serverIsOpen(GameType gameMode, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        if (WorldHostClient.wsClient != null) {
            WorldHostClient.wsClient.publishedWorld(WorldHostData.friends);
        }
    }

    @Inject(method = "halt", at = @At("TAIL"))
    private void serverIsClosed(boolean waitForServer, CallbackInfo ci) {
        WorldHostClient.CONNECTED_PROXY_CLIENTS.values().forEach(ProxyClient::close);
        WorldHostClient.CONNECTED_PROXY_CLIENTS.clear();
        if (isPublished() && WorldHostClient.wsClient != null) {
            WorldHostClient.wsClient.closedWorld(WorldHostData.friends);
        }
    }
}
