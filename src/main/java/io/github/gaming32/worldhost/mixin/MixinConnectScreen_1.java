package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class MixinConnectScreen_1 {
    @Shadow
    @Final
    ServerAddress val$hostAndPort;

    @Shadow
    @Final
    ConnectScreen this$0;

    @Inject(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;execute(Ljava/lang/Runnable;)V"
        ),
        cancellable = true
    )
    private void overrideError(CallbackInfo ci) {
        if (WorldHost.protoClient == null || val$hostAndPort.getHost().endsWith(WorldHost.protoClient.getBaseIp())) {
            return;
        }
        final Long attemptingToJoin = WorldHost.protoClient.getAttemptingToJoin();
        if (attemptingToJoin == null) return;
        Minecraft.getInstance().execute(() -> WorldHost.connect(
            this$0, attemptingToJoin,
            WorldHost.connectionIdToString(attemptingToJoin) + '.' + WorldHost.protoClient.getBaseIp(),
            WorldHost.protoClient.getBasePort()
        ));
        ci.cancel();
    }
}
