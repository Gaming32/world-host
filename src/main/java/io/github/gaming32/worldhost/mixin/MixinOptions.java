package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.testing.WorldHostTesting;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Options.class)
public class MixinOptions {
    @Shadow public boolean pauseOnLostFocus;

    @Shadow public boolean onboardAccessibility;

    @Inject(method = "load", at = @At("RETURN"))
    private void dontPauseWhileTesting(CallbackInfo ci) {
        if (WorldHostTesting.ENABLED) {
            pauseOnLostFocus = false;
            onboardAccessibility = false;
        }
    }
}
