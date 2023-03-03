package io.github.gaming32.worldhost.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.gaming32.worldhost.ext.TutorialToastExt;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;

@Mixin(TutorialToast.class)
public class MixinTutorialToast implements TutorialToastExt {
    private IconRenderer customIcon;

    @Override
    public void setCustomIcon(@NotNull IconRenderer customIcon) {
        this.customIcon = Objects.requireNonNull(customIcon);
    }

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/toast/TutorialToast$Type;drawIcon(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/gui/DrawableHelper;II)V"
        )
    )
    private void drawCustomIcon(TutorialToast.Type instance, MatrixStack matrices, DrawableHelper helper, int x, int y, Operation<Void> original) {
        if (customIcon != null) {
            customIcon.draw(matrices, x, y);
        } else {
            original.call(instance, matrices, helper, x, y);
        }
    }
}
