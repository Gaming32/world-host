package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.gui.OnlineStatusButton;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
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
//#else
//$$ import net.minecraftforge.internal.BrandingControl;
//$$ import java.util.function.BiConsumer;
//#endif

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void onlineStatus(CallbackInfo ci) {
        final OnlineStatusLocation location = WorldHost.CONFIG.getOnlineStatusLocation();
        if (location == OnlineStatusLocation.OFF) return;
        int y = 20;
        //#if FABRIC
        //#if MC >= 11802
        final int mmcLines = WorldHost.getMMCLines(false);
        if (mmcLines > 0) {
            y += mmcLines * 12;
        }
        //#endif
        //#if MC <= 11601
        //$$ if (FabricLoader.getInstance().isModLoaded("modmenu")) {
        //$$     y += 12;
        //$$ }
        //#endif
        //#else
        //$$ int[] forgeLineCount = {-1};
        //$$ final BiConsumer<Integer, String> lineConsumer = (i, s) -> forgeLineCount[0]++;
        //$$ if (location == OnlineStatusLocation.LEFT) {
        //$$     BrandingControl.forEachLine(true, true, lineConsumer);
        //$$ } else {
        //$$     BrandingControl.forEachAboveCopyrightLine(lineConsumer);
        //$$     forgeLineCount[0]++;
        //$$ }
        //$$ y += forgeLineCount[0] * 10;
        //#endif
        addRenderableWidget(new OnlineStatusButton(
            location == OnlineStatusLocation.RIGHT ? width - 2 : 2,
            height - y,
            10, location == OnlineStatusLocation.RIGHT, font
        ));
    }

    //#if MC <= 11605
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif
}
