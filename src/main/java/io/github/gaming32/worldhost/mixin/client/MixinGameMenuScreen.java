package io.github.gaming32.worldhost.mixin.client;

import net.minecraft.client.gui.screen.GameMenuScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GameMenuScreen.class)
public class MixinGameMenuScreen {
    @ModifyConstant(method = "initWidgets", constant = @Constant(stringValue = "menu.shareToLan"))
    private String changeLabel(String constant) {
        return "world-host.open_world";
    }
}
