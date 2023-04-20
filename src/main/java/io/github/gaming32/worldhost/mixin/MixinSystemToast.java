package io.github.gaming32.worldhost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.DeferredToastManager;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(SystemToast.class)
public class MixinSystemToast {
    @Shadow
    @Final
    @Mutable
    private int width;
    private DeferredToastManager.IconRenderer customIcon;

    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/components/toasts/SystemToast$SystemToastIds;Lnet/minecraft/network/chat/Component;Ljava/util/List;I)V",
        at = @At("TAIL")
    )
    private void customIconSupport(SystemToast.SystemToastIds type, Component title, List<FormattedCharSequence> lines, int width, CallbackInfo ci) {
        customIcon = DeferredToastManager.queuedCustomIcon;
        DeferredToastManager.queuedCustomIcon = null;
        if (customIcon != null) {
            this.width += 12;
        }
    }

    @ModifyConstant(method = "render", constant = @Constant(floatValue = 18))
    private float moveTextRight(float constant) {
        return customIcon != null ? constant + 12 : constant;
    }

    @Inject(
        method = "render",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/components/toasts/SystemToast;messageLines:Ljava/util/List;",
            ordinal = 1
        )
    )
    private void drawCustomIcon(PoseStack matrices, ToastComponent manager, long startTime, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (customIcon != null) {
            customIcon.draw(matrices, 6, 6);
        }
    }
}
