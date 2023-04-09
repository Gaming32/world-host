package io.github.gaming32.worldhost._1_19_4.mixin.client;

import io.github.gaming32.worldhost.common.DeferredToastManager;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void deferredToastReady(Overlay loadingGui, CallbackInfo ci) {
        if (loadingGui == null) {
            DeferredToastManager.ready();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickEvent(CallbackInfo ci) {
        WorldHostCommon.TICK_HANDLER.accept((Minecraft)(Object)this);
    }
}
