package io.github.gaming32.worldhost.mixin.client;

import io.github.gaming32.worldhost.client.DeferredToastManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void deferredToastReady(Overlay overlay, CallbackInfo ci) {
        if (overlay == null) {
            DeferredToastManager.ready();
        }
    }
}
