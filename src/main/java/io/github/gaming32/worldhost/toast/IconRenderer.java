package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
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

    static IconRenderer createSkinIconRenderer(ResourceLocation skinTexture) {
        return (context, x, y, width, height) -> {
            RenderSystem.enableBlend();
            WorldHostScreen.blit(context, skinTexture, x, y, width, height, 8, 8, 8, 8, 64, 64);
            WorldHostScreen.blit(context, skinTexture, x, y, width, height, 40, 8, 8, 8, 64, 64);
        };
    }
}
