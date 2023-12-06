package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelSummary.class)
public class MixinLevelSummary {
    //#if MC >= 1.20.3
    @Inject(method = "primaryActionMessage", at = @At("HEAD"), cancellable = true)
    private void shareButton(CallbackInfoReturnable<Component> cir) {
        if (WorldHost.CONFIG.isShareButton()) {
            cir.setReturnValue(WorldHostComponents.PLAY_TEXT);
        }
    }
    //#endif
}
