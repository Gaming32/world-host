package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.OnlineStatusButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 11605
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

//#if FABRIC && MC <= 11601
//$$ import net.fabricmc.loader.api.FabricLoader;
//#endif

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onlineStatus(CallbackInfo ci) {
        if (!WorldHost.CONFIG.isShowOnlineStatus()) return;
        addRenderableWidget(new OnlineStatusButton(
            width - 2,
            height -
                //#if FABRIC && MC <= 11601
                //$$ (FabricLoader.getInstance().isModLoaded("modmenu") ? 32 : 20),
                //#elseif FORGE
                //$$ 30,
                //#else
                20,
                //#endif
            10, font
        ));
    }

    //#if MC <= 11605
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif
}
