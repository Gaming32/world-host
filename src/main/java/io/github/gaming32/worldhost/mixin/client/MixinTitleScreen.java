package io.github.gaming32.worldhost.mixin.client;

import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(
        method = "render",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V"
        )
    )
    private void showOnlineStatus(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        final Text text = Text.empty()
            .append(Text.literal("\u25cf").formatted(WorldHostClient.wsClient != null ? Formatting.DARK_GREEN : Formatting.RED))
            .append(" World Host: " + (WorldHostClient.wsClient != null ? "Online" : "Offline"));
        final int textWidth = textRenderer.getWidth(text);
        drawTextWithShadow(matrices, textRenderer, text, width - textWidth - 2, height - 20, 0xffffffff);
    }
}
