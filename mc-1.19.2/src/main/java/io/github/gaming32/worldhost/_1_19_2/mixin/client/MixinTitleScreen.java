package io.github.gaming32.worldhost._1_19_2.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Component title) {
        super(title);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"
        )
    )
    private void showOnlineStatus(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        final Component text = Component.empty()
            .append(Component.literal("\u25cf").withStyle(WorldHostCommon.wsClient != null ? ChatFormatting.DARK_GREEN : ChatFormatting.RED))
            .append(" World Host: " + (WorldHostCommon.wsClient != null ? "Online" : "Offline"));
        final int textWidth = font.width(text);
        drawString(matrices, font, text, width - textWidth - 2, height - 20, 0xffffffff);
    }
}
