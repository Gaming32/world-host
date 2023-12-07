package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.ext.SelectWorldScreenExt;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SelectWorldScreen.class)
public class MixinSelectWorldScreen extends Screen implements SelectWorldScreenExt {
    @Shadow private WorldSelectionList list;

    @Unique
    private Button wh$shareButton;

    @Unique
    private boolean wh$shareButtonPressed;

    protected MixinSelectWorldScreen(Component component) {
        super(component);
    }

    //#if MC >= 1.20.3
    @ModifyArg(
        method = "init()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;",
            ordinal = 0
        )
    )
    private Component changePlayButtonText(Component original) {
        return WorldHost.CONFIG.isShareButton() ? WorldHostComponents.PLAY_TEXT : original;
    }
    //#else
    //$$ @ModifyConstant(method = "init()V", constant = @Constant(stringValue = "selectWorld.select"))
    //$$ private String changePlayButtonText(String constant) {
    //$$     return WorldHost.CONFIG.isShareButton() ? "world-host.play_world" : constant;
    //$$ }
    //#endif

    @ModifyConstant(method = "init()V", constant = @Constant(intValue = 150, ordinal = 0))
    private int shrinkPlayButton(int constant) {
        return WorldHost.CONFIG.isShareButton() ? 100 : constant;
    }

    @Inject(
        method = "init()V",
        at = @At(
            value = "INVOKE",
            //#if MC > 1.16.5
            target = "Lnet/minecraft/client/gui/screens/worldselection/SelectWorldScreen;addRenderableWidget(Lnet/minecraft/client/gui/components/events/GuiEventListener;)Lnet/minecraft/client/gui/components/events/GuiEventListener;",
            //#else
            //$$ target = "Lnet/minecraft/client/gui/screens/worldselection/SelectWorldScreen;addButton(Lnet/minecraft/client/gui/components/AbstractWidget;)Lnet/minecraft/client/gui/components/AbstractWidget;",
            //#endif
            ordinal = 0,
            shift = At.Shift.AFTER
        )
    )
    private void addShareWorldButton(CallbackInfo ci) {
        if (!WorldHost.CONFIG.isShareButton()) {
            wh$shareButton = null;
            return;
        }
        //#if MC > 1.16.5
        wh$shareButton = addRenderableWidget(
        //#else
        //$$ wh$shareButton = addButton(
        //#endif
            WorldHostScreen.button(Components.translatable("world-host.share_world"), b ->
                list.getSelectedOpt().ifPresent(worldListEntry -> {
                    wh$shareButtonPressed = true;
                    worldListEntry.joinWorld();
                })
            ).pos(width / 2 - 50, height - 52)
                .width(100)
                .build()
        );
    }

    @ModifyConstant(method = "init()V", constant = @Constant(intValue = 4, ordinal = 0))
    private int moveCreateButton(int constant) {
        return WorldHost.CONFIG.isShareButton() ? 54 : constant;
    }

    @ModifyConstant(method = "init()V", constant = @Constant(intValue = 150, ordinal = 1))
    private int shrinkCreateButton(int constant) {
        return WorldHost.CONFIG.isShareButton() ? 100 : constant;
    }

    @ModifyConstant(method = "init()V", constant = @Constant(stringValue = "selectWorld.create"))
    private String changeCreateButtonText(String constant) {
        return WorldHost.CONFIG.isShareButton() ? "world-host.create_world" : constant;
    }

    //#if MC >= 1.20.3
    @ModifyArg(
        method = "updateButtonStatus",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button;setMessage(Lnet/minecraft/network/chat/Component;)V",
            ordinal = 0
        )
    )
    private Component changePlayButtonTextOnUpdate(Component original) {
        return WorldHost.CONFIG.isShareButton() ? WorldHostComponents.PLAY_TEXT : original;
    }
    //#endif

    @Inject(method = "updateButtonStatus", at = @At("TAIL"))
    //#if MC >= 1.20.3
    private void updateShareButtonStatus(LevelSummary levelSummary, CallbackInfo ci) {
        if (wh$shareButton != null) {
            wh$shareButton.active = levelSummary != null && levelSummary.primaryActionActive();
        }
    }
    //#else
    //$$ private void updateShareButtonStatus(
    //$$     boolean active,
        //#if MC > 1.19.2
        //$$ boolean bl2,
        //#endif
    //$$     CallbackInfo ci
    //$$ ) {
    //$$     if (wh$shareButton != null) {
    //$$         wh$shareButton.active = active;
    //$$     }
    //$$ }
    //#endif

    @Override
    public boolean wh$shareButtonPressed() {
        return wh$shareButtonPressed;
    }
}
