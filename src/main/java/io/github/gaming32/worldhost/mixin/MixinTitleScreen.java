package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.widget.OnlineStatusButton;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onlineStatus(CallbackInfo ci) {
        final OnlineStatusLocation location = WorldHost.CONFIG.getOnlineStatusLocation();
        if (location == OnlineStatusLocation.OFF) return;
        final int y = 20 + WorldHost.getMenuLines(false) * WorldHost.getMainMenuLineSpacing();
        addRenderableWidget(new OnlineStatusButton(
            location == OnlineStatusLocation.RIGHT ? width - 2 : 2,
            height - y,
            10, location == OnlineStatusLocation.RIGHT, font
        ));
    }
}
