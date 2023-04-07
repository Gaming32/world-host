package io.github.gaming32.worldhost.mixin.client;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {
    @ModifyConstant(method = "createPauseMenu", constant = @Constant(stringValue = "menu.shareToLan"))
    private String changeLabel(String constant) {
        return "world-host.open_world";
    }
}
