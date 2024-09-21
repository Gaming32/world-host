package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.screen.JoiningWorldHostScreen;
import io.github.gaming32.worldhost.testing.ScreenChain;
import io.github.gaming32.worldhost.testing.WorldHostTesting;
import io.github.gaming32.worldhost.toast.WHToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC < 1.19.4
//$$ import com.mojang.blaze3d.platform.Window;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.MouseHandler;
//$$ import net.minecraft.client.Timer;
//$$ import org.spongepowered.asm.mixin.Final;
//$$ import org.spongepowered.asm.mixin.Shadow;
//#endif

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    //#if MC < 1.19.4
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

    @Shadow public Screen screen;

    @Unique
    private Class<? extends Screen> wh$lastScreenClass;

    @Inject(method = "setOverlay", at = @At("HEAD"))
    private void deferredToastReady(Overlay loadingGui, CallbackInfo ci) {
        if (loadingGui == null) {
            WHToast.ready();
            WorldHost.clientLoadedFully = true;
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        WHToast.tick();
        final var screenClass = ScreenChain.getScreenClass(screen);
        if (WorldHostTesting.ENABLED && WorldHost.clientLoadedFully && screenClass != wh$lastScreenClass) {
            wh$lastScreenClass = screenClass;
            WorldHostTesting.SCREEN_CHAIN.get().advance(screen);
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void postTick(CallbackInfo ci) {
        WorldHost.tickHandler();
    }

    //#if MC < 1.19.4
    //$$ @Inject(
    //$$     method = "runTick",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
    //$$         shift = At.Shift.AFTER
    //$$     )
    //$$ )
    //$$ private void toastRender(CallbackInfo ci) {
    //$$     int i = (int)(
    //$$         this.mouseHandler.xpos() * (double)this.getWindow().getGuiScaledWidth() / (double)this.getWindow().getScreenWidth()
    //$$     );
    //$$     int j = (int)(
    //$$         this.mouseHandler.ypos() * (double)this.getWindow().getGuiScaledHeight() / (double)this.getWindow().getScreenHeight()
    //$$     );
    //$$     WHToast.render(new PoseStack(), i, j, pause ? pausePartialTick : timer.partialTick);
    //$$ }
    //#endif

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void clearCurrentlyConnecting(Screen guiScreen, CallbackInfo ci) {
        if (
            !(guiScreen instanceof ConnectScreen) &&
            !(guiScreen instanceof JoiningWorldHostScreen) &&
            !(guiScreen instanceof ProgressScreen) &&
            WorldHost.protoClient != null
        ) {
            WorldHost.protoClient.setAttemptingToJoin(null);
        }
    }
}
