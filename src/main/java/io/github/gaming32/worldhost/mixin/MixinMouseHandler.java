package io.github.gaming32.worldhost.mixin;

import com.mojang.blaze3d.platform.Window;
import io.github.gaming32.worldhost.toast.WHToast;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.GLFW_PRESS;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {
    @Shadow @Final private Minecraft minecraft;

    @Shadow private double xpos;

    @Shadow private double ypos;

    @Inject(
        method = "onPress",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;",
            ordinal = 0
        ),
        cancellable = true
    )
    private void toastClick(long windowPointer, int button, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW_PRESS) return;
        final Window window = minecraft.getWindow();
        if (WHToast.click(
            xpos * window.getGuiScaledWidth() / window.getScreenWidth(),
            ypos * window.getGuiScaledHeight() / window.getScreenHeight()
        )) {
            ci.cancel();
        }
    }
}
