package io.github.gaming32.worldhost.mixin.modmenu;

import org.spongepowered.asm.mixin.Mixin;

//#if FABRIC && MC > 11601
import com.terraformersmc.modmenu.event.ModMenuEventHandler;
import io.github.gaming32.worldhost.gui.OnlineStatusButton;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 11904
import net.minecraft.client.gui.layouts.LayoutElement;
//#else
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

@Mixin(ModMenuEventHandler.class)
public class MixinModMenuEventHandler {
    //#if MC >= 11904
    @Inject(method = "shiftButtons", at = @At("HEAD"), cancellable = true, require = 0)
    private static void dontShiftOnlineStatus(LayoutElement widget, boolean shiftUp, int spacing, CallbackInfo ci) {
        if (widget instanceof OnlineStatusButton) {
            ci.cancel();
        }
    }
    //#else
    //$$ @Inject(method = "shiftButtons", at = @At("HEAD"), cancellable = true)
    //$$ private static void dontShiftOnlineStatus(AbstractWidget button, boolean shiftUp, int spacing, CallbackInfo ci) {
    //$$     if (button instanceof OnlineStatusButton) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
//#else
//$$ // Fallback so that there is *some* mixin class to load
//$$ import net.minecraft.client.Minecraft;
//$$
//$$ @Mixin(Minecraft.class)
//$$ public class MixinModMenuEventHandler {
//$$ }
//#endif
