package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.vertex.PoseStack;

@FunctionalInterface
public interface IconRenderer {
    void draw(PoseStack matrices, int x, int y, int width, int height);
}
