package io.github.gaming32.worldhost.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.gaming32.worldhost.testing.WorldHostTesting;
import io.github.gaming32.worldhost.testing.WindowCallbackManager;
import org.lwjgl.glfw.GLFWCharModsCallbackI;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWDropCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWMouseButtonCallbackI;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InputConstants.class)
public class MixinInputConstants {
    @Inject(method = "setupKeyboardCallbacks", at = @At("HEAD"), cancellable = true)
    private static void mockKeyboardCallbacks(
        long window,
        GLFWKeyCallbackI keyCallback,
        GLFWCharModsCallbackI charModifierCallback,
        CallbackInfo ci
    ) {
        if (!WorldHostTesting.ENABLED) return;
        WindowCallbackManager.keyCallback = keyCallback;
        WindowCallbackManager.charModsCallback = charModifierCallback;
        ci.cancel();
    }

    @Inject(method = "setupMouseCallbacks", at = @At("HEAD"), cancellable = true)
    private static void mockMouseCallbacks(
        long window,
        GLFWCursorPosCallbackI cursorPositionCallback,
        GLFWMouseButtonCallbackI mouseButtonCallback,
        GLFWScrollCallbackI scrollCallback,
        GLFWDropCallbackI dragAndDropCallback,
        CallbackInfo ci
    ) {
        if (!WorldHostTesting.ENABLED) return;
        WindowCallbackManager.cursorPosCallback = cursorPositionCallback;
        WindowCallbackManager.mouseButtonCallback = mouseButtonCallback;
        WindowCallbackManager.scrollCallback = scrollCallback;
        WindowCallbackManager.dropCallback = dragAndDropCallback;
        ci.cancel();
    }
}
