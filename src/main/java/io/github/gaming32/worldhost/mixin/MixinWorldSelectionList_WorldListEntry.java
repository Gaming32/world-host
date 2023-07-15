package io.github.gaming32.worldhost.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 1.16.5
//$$ import org.lwjgl.glfw.GLFW;
//#endif

@Mixin(WorldSelectionList.WorldListEntry.class)
public class MixinWorldSelectionList_WorldListEntry {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "joinWorld", at = @At("HEAD"))
    private void shareWorldOnShift(CallbackInfo ci) {
        if (
            !WorldHost.CONFIG.isShareButton() ||
                WorldHost.shareWorldOnLoadUi ||
                !InputConstants.isKeyDown(
                    minecraft.getWindow().getWindow(),
                    //#if MC > 1.16.5
                    InputConstants.KEY_LSHIFT
                    //#else
                    //$$ GLFW.GLFW_KEY_LEFT_SHIFT
                    //#endif
                )
        ) return;
        WorldHost.shareWorldOnLoadUi = true;
    }

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void setupShareWorld(CallbackInfo ci) {
        WorldHost.shareWorldOnLoadReal = WorldHost.shareWorldOnLoadUi;
        WorldHost.shareWorldOnLoadUi = false;
    }
}
