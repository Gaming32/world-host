package io.github.gaming32.worldhost._1_19_4.mixin.client;

import io.github.gaming32.worldhost.common.WorldHostData;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {
    @ModifyConstant(method = "<clinit>", constant = @Constant(stringValue = "menu.shareToLan"))
    private static String changeLabel(String constant) {
        return WorldHostData.enableFriends ? "world-host.open_world" : "world-host.open_world_no_friends";
    }
}
