package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC > 1.16.5
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
//#endif

@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreen_1 {
    @Unique
    private ConnectScreen wh$parent;

    @Unique
    private String wh$host;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initRefs(
        ConnectScreen connectScreen, String string,
        //#if MC > 1.16.5
        ServerAddress serverAddress, Minecraft minecraft, ServerData serverData,
        //#else
        //$$ String host, int port,
        //#endif
        CallbackInfo ci
    ) {
        wh$parent = connectScreen;
        //#if MC > 1.16.5
        wh$host = serverAddress.getHost();
        //#else
        //$$ wh$host = host;
        //#endif
    }

    @Inject(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"
        ),
        cancellable = true
    )
    private void overrideError(CallbackInfo ci) {
        if (WorldHost.protoClient == null || wh$host.endsWith(WorldHost.protoClient.getBaseIp())) {
            return;
        }
        final Long attemptingToJoin = WorldHost.protoClient.getAttemptingToJoin();
        if (attemptingToJoin == null) return;
        Minecraft.getInstance().execute(() -> WorldHost.connect(
            wh$parent, attemptingToJoin,
            WorldHost.connectionIdToString(attemptingToJoin) + '.' + WorldHost.protoClient.getBaseIp(),
            WorldHost.protoClient.getBasePort()
        ));
        ci.cancel();
    }
}
