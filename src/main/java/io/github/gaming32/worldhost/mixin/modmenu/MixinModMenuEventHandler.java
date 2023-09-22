//#if FABRIC && MC > 1.16.1
package io.github.gaming32.worldhost.mixin.modmenu;

import com.terraformersmc.modmenu.event.ModMenuEventHandler;
import io.github.gaming32.worldhost.gui.widget.OnlineStatusButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 1.19.4
import net.minecraft.client.gui.layouts.LayoutElement;
//#else
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

@Mixin(value = ModMenuEventHandler.class, remap = false)
public class MixinModMenuEventHandler {
    //#if MC >= 1.19.4
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
//#endif
