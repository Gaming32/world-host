package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.compat.WorldHostSimpleVoiceChatCompat;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Inject(method = "tickServer", at = @At("RETURN"))
    private void tickVoiceChat(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        if (WorldHost.isModLoaded("voicechat")) {
            WorldHostSimpleVoiceChatCompat.tick((MinecraftServer)(Object)this);
        }
    }
}
