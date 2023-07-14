package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreen_1 {
    @Unique
    private ConnectScreen wh$parent;

    @Unique
    private ServerAddress wh$address;

    @Inject(method = "<init>", at = @At("TAIL"))
    @SuppressWarnings("InvalidInjectorMethodSignature") // mcdev thinks that the ServerData should be a CompletableFuture for some reason
    private void initRefs(ConnectScreen connectScreen, String string, ServerAddress serverAddress, Minecraft minecraft, ServerData serverData, CallbackInfo ci) {
        wh$parent = connectScreen;
        wh$address = serverAddress;
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
        if (WorldHost.protoClient == null || wh$address.getHost().endsWith(WorldHost.protoClient.getBaseIp())) {
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
