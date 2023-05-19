package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.toast.WHToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC < 11904
//$$ import com.mojang.blaze3d.platform.Window;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.MouseHandler;
//$$ import net.minecraft.client.Timer;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Shadow;
//#endif

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    //#if MC < 11904
    //$$ @Shadow @Final public MouseHandler mouseHandler;
    //$$
    //$$ @Shadow private boolean pause;
    //$$
    //$$ @Shadow private float pausePartialTick;
    //$$
    //$$ @Shadow @Final private Timer timer;
    //$$
    //$$ @Shadow public abstract Window getWindow();
    //#endif

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void deferredToastReady(Overlay loadingGui, CallbackInfo ci) {
        if (loadingGui == null) {
            WHToast.ready();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void toastTick(CallbackInfo ci) {
        WHToast.tick();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickEvent(CallbackInfo ci) {
        WorldHost.tickHandler();
    }

    //#if MC < 11904
    //$$ @Inject(
    //$$     method = "runTick",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
    //$$         shift = At.Shift.AFTER
    //$$     )
    //$$ )
    //$$ private void toastRender(boolean renderLevel, CallbackInfo ci) {
    //$$     int i = (int)(
    //$$         this.mouseHandler.xpos() * (double)this.getWindow().getGuiScaledWidth() / (double)this.getWindow().getScreenWidth()
    //$$     );
    //$$     int j = (int)(
    //$$         this.mouseHandler.ypos() * (double)this.getWindow().getGuiScaledHeight() / (double)this.getWindow().getScreenHeight()
    //$$     );
    //$$     WHToast.render(new PoseStack(), i, j, pause ? pausePartialTick : timer.partialTick);
    //$$ }
    //#endif
}
