package io.github.gaming32.worldhost.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;

//#if MC >= 1.19.4
//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderBuffers;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif
import io.github.gaming32.worldhost.toast.WHToast;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//#endif

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    //#if MC >= 1.19.4
    @Shadow @Final Minecraft minecraft;

    //#if MC >= 1.20.0
    @Shadow @Final private RenderBuffers renderBuffers;
    //#endif

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            //#if MC >= 1.20.0
            target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lnet/minecraft/client/gui/GuiGraphics;)V",
            //#else
            //$$ target = "Lnet/minecraft/client/gui/components/toasts/ToastComponent;render(Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            //#endif
            shift = At.Shift.AFTER
        )
    )
    private void toastRender(CallbackInfo ci) {
        int mouseX = (int)(
            this.minecraft.mouseHandler.xpos() * (double)this.minecraft.getWindow().getGuiScaledWidth() / (double)this.minecraft.getWindow().getScreenWidth()
        );
        int mouseY = (int)(
            this.minecraft.mouseHandler.ypos() * (double)this.minecraft.getWindow().getGuiScaledHeight() / (double)this.minecraft.getWindow().getScreenHeight()
        );
        WHToast.render(
            //#if MC < 1.20.0
            //$$ new PoseStack(),
            //#else
            new GuiGraphics(minecraft, renderBuffers.bufferSource()),
            //#endif
            mouseX, mouseY, minecraft.getFrameTime()
        );
    }
    //#endif
}
