package io.github.gaming32.worldhost.ext;

import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;

public interface TutorialToastExt {
    @FunctionalInterface
    interface IconRenderer {
        void draw(MatrixStack matrices, int x, int y);
    }

    void setCustomIcon(@NotNull IconRenderer renderer);
}
