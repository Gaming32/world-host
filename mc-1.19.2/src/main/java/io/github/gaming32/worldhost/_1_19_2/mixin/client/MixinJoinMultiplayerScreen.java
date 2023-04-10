package io.github.gaming32.worldhost._1_19_2.mixin.client;

import io.github.gaming32.worldhost._1_19_2.gui.FriendsButtonWidget;
import io.github.gaming32.worldhost._1_19_2.gui.OnlineFriendsScreen;
import io.github.gaming32.worldhost.common.WorldHostData;
import io.github.gaming32.worldhost.common.WorldHostTexts;
import net.minecraft.client.gui.components.Button;
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

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen extends Screen {
    @Shadow @Final private Screen lastScreen;

    protected MixinJoinMultiplayerScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tabs(CallbackInfo ci) {
        if (!WorldHostData.enableFriends) return;

        addRenderableWidget(new Button(
            width / 2 - 102, 32, 100, 20, WorldHostTexts.SERVERS,
            button -> {}
        )).active = false;

        addRenderableWidget(new FriendsButtonWidget(
            width / 2 + 2, 32, 100, 20,
            button -> {
                assert minecraft != null;
                minecraft.setScreen(new OnlineFriendsScreen(lastScreen));
            }
        ));
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 32))
    private int makeTopBigger(int constant) {
        return WorldHostData.enableFriends ? 60 : constant;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 20))
    private int moveTitleUp(int constant) {
        return WorldHostData.enableFriends ? 15 : constant;
    }
}
