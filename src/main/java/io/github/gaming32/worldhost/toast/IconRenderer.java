package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

@FunctionalInterface
public interface IconRenderer {
    void draw(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int x, int y, int width, int height
    );

    static IconRenderer createSkinIconRenderer(Supplier<ResourceLocation> skinTexture) {
        return (context, x, y, width, height) -> {
            final var texture = skinTexture.get();
            RenderSystem.enableBlend();
            WorldHostScreen.blit(context, texture, x, y, 8, 8, width, height, 8, 8, 64, 64);
            WorldHostScreen.blit(context, texture, x, y, 40, 8, width, height, 8, 8, 64, 64);
        };
    }
}
