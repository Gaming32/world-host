package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;

@Mixin(Connection.class)
public class MixinConnection {
    @Shadow private Channel channel;

    @Inject(method = "isMemoryConnection", at = @At("HEAD"), cancellable = true)
    private void proxyConnectionDoesntCountAsMemory(CallbackInfoReturnable<Boolean> cir) {
        if (channel == null) return; // Fake players
        final SocketAddress checkAddress = WorldHost.proxySocketAddress;
        if (checkAddress == null) return;
        if (checkAddress.equals(channel.localAddress()) || checkAddress.equals(channel.remoteAddress())) {
            cir.setReturnValue(false);
        }
    }
}
