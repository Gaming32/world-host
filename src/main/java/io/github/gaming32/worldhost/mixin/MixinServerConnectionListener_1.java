package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import net.minecraft.server.network.ServerConnectionListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.network.ServerConnectionListener$1")
public abstract class MixinServerConnectionListener_1 extends ChannelInitializer<Channel> {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void storeClass(ServerConnectionListener this$0, CallbackInfo ci) throws NoSuchMethodException {
        if (WorldHost.channelInitializerConstructor == null) {
            WorldHost.channelInitializerConstructor = getClass().getDeclaredConstructor(ServerConnectionListener.class);
        }
    }
}
