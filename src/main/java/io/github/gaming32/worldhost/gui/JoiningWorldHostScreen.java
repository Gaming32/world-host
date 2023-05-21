package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

public class JoiningWorldHostScreen extends WorldHostScreen {
    public final Screen parent;

    public JoiningWorldHostScreen(Screen parent) {
        super(Components.translatable("world-host.joining_world_host"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(
            button(CommonComponents.GUI_CANCEL, b -> onClose())
                .pos(width / 2 - 100, height / 4 + 132)
                .width(200)
                .build()
        );
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 50, 0xffffff);
        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.setAttemptingToJoin(null);
        }
    }
}
