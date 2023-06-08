package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.OnlineStatusButton;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 1.16.5
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

@Mixin(PauseScreen.class)
public class MixinPauseScreen extends Screen {
    protected MixinPauseScreen(Component component) {
        super(component);
    }

    @ModifyConstant(
        method =
            //#if MC >= 1.19.4
            "<clinit>",
            //#else
            //$$ "createPauseMenu",
            //#endif
        constant = @Constant(stringValue = "menu.shareToLan")
    )
    private static String changeLabel(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onlineStatus(CallbackInfo ci) {
        final OnlineStatusLocation location = WorldHost.CONFIG.getOnlineStatusLocation();
        if (location == OnlineStatusLocation.OFF) return;
        int x = 7;
        int y = 15;
        //#if FABRIC && MC >= 1.18.2
        final int mmcLines = WorldHost.getMMCLines(true);
        if (mmcLines > 0) {
            x = 2;
            y = 10 + mmcLines * 12;
        }
        //#endif
        addRenderableWidget(new OnlineStatusButton(
            location == OnlineStatusLocation.RIGHT ? width - x : x,
            height - y,
            10,
            location == OnlineStatusLocation.RIGHT,
            font
        ));
    }

    //#if MC <= 1.16.5
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif
}
