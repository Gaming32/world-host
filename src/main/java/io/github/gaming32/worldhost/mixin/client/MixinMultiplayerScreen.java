package io.github.gaming32.worldhost.mixin.client;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.gui.FriendsButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public class MixinMultiplayerScreen extends Screen {
    protected MixinMultiplayerScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void tabs(CallbackInfo ci) {
        addDrawableChild(new ButtonWidget(
            width / 2 - 102, 32, 100, 20, WorldHostTexts.SERVERS,
            button -> {}
        )).active = false;

        addDrawableChild(new FriendsButtonWidget(
            width / 2 + 2, 32, 100, 20,
            button -> {
                WorldHost.LOGGER.info("Clicked Friends tab");
            }
        ));
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 32))
    private int makeTopBigger(int constant) {
        return 60;
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 20))
    private int moveTitleUp(int constant) {
        return 15;
    }
}
