package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import io.github.gaming32.worldhost.gui.widget.OnlineStatusButton;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinPauseScreen extends Screen {
    protected MixinPauseScreen(Component component) {
        super(component);
    }

    //#if MC >= 1.19.4
    @Redirect(
        method = "createPauseMenu",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/gui/screens/PauseScreen;SHARE_TO_LAN:Lnet/minecraft/network/chat/Component;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private Component changeLabel() {
        return Component.translatable(
            WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends"
        );
    }
    //#else
    //$$ @ModifyConstant(method = "createPauseMenu", constant = @Constant(stringValue = "menu.shareToLan"))
    //$$ private static String changeLabel(String constant) {
    //$$     return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    //$$ }
    //#endif

    @Inject(method = "init", at = @At("RETURN"))
    private void onlineStatus(CallbackInfo ci) {
        final OnlineStatusLocation location = WorldHost.CONFIG.getOnlineStatusLocation();
        if (location == OnlineStatusLocation.OFF) return;
        int x = 7;
        int y = 15;
        final int mmcLines = WorldHost.getMMCLines(true);
        if (mmcLines > 0) {
            x = 2;
            y = 10 + mmcLines * 12;
        }
        addRenderableWidget(new OnlineStatusButton(
            location == OnlineStatusLocation.RIGHT ? width - x : x,
            height - y,
            10,
            location == OnlineStatusLocation.RIGHT,
            font
        ));
    }
}
