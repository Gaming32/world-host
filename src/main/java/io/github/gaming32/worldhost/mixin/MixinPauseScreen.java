package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {
    @ModifyConstant(
        method =
            //#if MC >= 11904
            "<clinit>",
            //#else
            //$$ "createPauseMenu",
            //#endif
        constant = @Constant(stringValue = "menu.shareToLan")
    )
    private static String changeLabel(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }
}
