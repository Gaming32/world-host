package io.github.gaming32.worldhost.mixin.client;

import io.github.gaming32.worldhost.client.DeferredToastManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
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
    @Shadow @Final @Mutable private int width;
    private DeferredToastManager.IconRenderer customIcon;

    @Inject(
        method = "<init>(Lnet/minecraft/client/toast/SystemToast$Type;Lnet/minecraft/text/Text;Ljava/util/List;I)V",
        at = @At("TAIL")
    )
    private void customIconSupport(SystemToast.Type type, Text title, List<OrderedText> lines, int width, CallbackInfo ci) {
        customIcon = DeferredToastManager.queuedCustomIcon;
        DeferredToastManager.queuedCustomIcon = null;
        if (customIcon != null) {
            this.width += 12;
        }
    }

    @ModifyConstant(method = "draw", constant = @Constant(floatValue = 18))
    private float moveTextRight(float constant) {
        return customIcon != null ? constant + 12 : constant;
    }

    @Inject(
        method = "draw",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/toast/SystemToast;lines:Ljava/util/List;",
            ordinal = 1
        )
    )
    private void drawCustomIcon(MatrixStack matrices, ToastManager manager, long startTime, CallbackInfoReturnable<Toast.Visibility> cir) {
        if (customIcon != null) {
            customIcon.draw(matrices, 6, 6);
        }
    }
}
