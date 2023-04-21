package io.github.gaming32.worldhost.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
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
    protected MixinTitleScreen(Component component) {
        super(component);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;render(Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"
        )
    )
    private void showOnlineStatus(PoseStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!WorldHost.CONFIG.isShowOnlineStatus()) return;
        final Component text = Components.empty()
            .append(Components.literal("\u25cf").withStyle(WorldHost.protoClient != null ? ChatFormatting.DARK_GREEN : ChatFormatting.RED))
            .append(" World Host: " + (WorldHost.protoClient != null ? "Online" : "Offline"));
        final int textWidth = font.width(text);
        drawString(matrices, font, text, width - textWidth - 2, height - 20, 0xffffffff);
    }
}
