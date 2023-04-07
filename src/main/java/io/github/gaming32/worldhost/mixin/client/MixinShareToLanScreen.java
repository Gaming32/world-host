package io.github.gaming32.worldhost.mixin.client;

import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ShareToLanScreen.class)
public class MixinShareToLanScreen {
    @ModifyConstant(method = "<init>", constant = @Constant(stringValue = "lanServer.title"))
    private static String changeLabelI1(String constant) {
        return "world-host.open_world";
    }

    @ModifyConstant(method = "init", constant = @Constant(stringValue = "lanServer.start"))
    private String changeLabelI2(String constant) {
        return "world-host.open_world";
    }
}
