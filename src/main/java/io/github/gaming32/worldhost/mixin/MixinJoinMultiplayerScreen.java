package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.widget.FriendsButton;
import io.github.gaming32.worldhost.gui.screen.OnlineFriendsScreen;
import io.github.gaming32.worldhost.versions.ButtonBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC <= 1.16.5
//$$ import net.minecraft.client.gui.components.AbstractWidget;
//#endif

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen extends Screen {
    @Shadow
    @Final
    private Screen lastScreen;

    protected MixinJoinMultiplayerScreen(Component component) {
        super(component);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tabs(CallbackInfo ci) {
        if (!WorldHost.CONFIG.isEnableFriends()) return;

        addRenderableWidget(
            new ButtonBuilder(WorldHostComponents.SERVERS, button -> {})
                .pos(width / 2 - 102, 32)
                .width(100)
                .build()
        ).active = false;

        addRenderableWidget(new FriendsButton(
            width / 2 + 2, 32, 100, 20,
            button -> {
                assert minecraft != null;
                minecraft.setScreen(new OnlineFriendsScreen(lastScreen));
            }
        ));
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 32))
    private int makeTopBigger(int constant) {
        return WorldHost.CONFIG.isEnableFriends() ? 60 : constant;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 20))
    private int moveTitleUp(int constant) {
        return WorldHost.CONFIG.isEnableFriends() ? 15 : constant;
    }

    //#if MC <= 1.16.5
    //$$ protected <T extends AbstractWidget> T addRenderableWidget(T widget) {
    //$$     return addButton(widget);
    //$$ }
    //#endif
}
