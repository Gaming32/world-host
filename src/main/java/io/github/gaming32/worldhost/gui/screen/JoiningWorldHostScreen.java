package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class JoiningWorldHostScreen extends WorldHostScreen {
    private static final Component MESSAGE = Components.translatable("world-host.joining_world_host");

    public final Screen parent;
    private int timeout = 20;

    public JoiningWorldHostScreen(Screen parent) {
        super(MESSAGE);
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
    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float delta
    ) {
        whRenderBackground(context, mouseX, mouseY, delta);
        drawCenteredString(context, font, MESSAGE, width / 2, height / 2 - 50, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.setAttemptingToJoin(null);
        }
    }

    @Override
    public void tick() {
        if (timeout-- == 0) {
            final Long attemptingToJoin = WorldHost.protoClient.getAttemptingToJoin();
            if (attemptingToJoin == null) {
                WorldHost.LOGGER.warn("attemptingToJoin was null after timeout in JoiningWorldHostScreen");
                assert minecraft != null;
                minecraft.setScreen(parent);
                return;
            }
            WorldHost.connect(parent, attemptingToJoin);
        }
    }
}
