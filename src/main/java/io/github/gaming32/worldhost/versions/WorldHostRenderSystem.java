package io.github.gaming32.worldhost.versions;

import com.mojang.blaze3d.systems.RenderSystem;

public class WorldHostRenderSystem {
    public static void enableBlend() {
        //#if MC < 1.21.5
        RenderSystem.enableBlend();
        //#endif
    }

    public static void disableBlend() {
        //#if MC < 1.21.5
        RenderSystem.disableBlend();
        //#endif
    }
}
