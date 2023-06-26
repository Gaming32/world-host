package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC > 1.16.5
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//#endif

@Mixin(ConnectScreen.class)
public class MixinConnectScreen {
    @Shadow @Final Screen parent;

    @Inject(method = "connect", at = @At("HEAD"), cancellable = true)
    //#if MC > 1.16.5
    private void overrideConnect(
            Minecraft minecraft, ServerAddress serverAddress,
            //#if MC > 1.19.2
            ServerData serverData,
            //#endif
            CallbackInfo ci
    ) {
        final String host = serverAddress.getHost();
        final int port = serverAddress.getPort();
    //#else
    //$$ private void overrideConnect(String host, int port, CallbackInfo ci) {
    //#endif
        if (WorldHost.protoClient == null) return;
        if (WorldHost.protoClient.getAttemptingToJoin() != null) {
            WorldHost.protoClient.setAttemptingToJoin(null);
            return;
        }

        final String targetBaseAddr;
        final int targetBasePort;
        if (WorldHost.proxyProtocolClient != null) {
            targetBaseAddr = WorldHost.proxyProtocolClient.getBaseAddr();
            targetBasePort = WorldHost.proxyProtocolClient.getMcPort();
        } else {
            targetBaseAddr = WorldHost.protoClient.getBaseIp();
            targetBasePort = WorldHost.protoClient.getBasePort();
        }

        if (port != targetBasePort) return;

        final int dotIndex = host.indexOf('.');
        if (dotIndex == -1) return;
        final String baseAddr = host.substring(dotIndex + 1);

        if (!baseAddr.equals(targetBaseAddr)) return;

        final Long cid = WorldHost.tryParseConnectionId(host.substring(0, dotIndex));
        if (cid == null) return;

        WorldHost.join(cid, parent);
        ci.cancel();
    }
}
