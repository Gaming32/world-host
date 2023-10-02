package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class JoiningWorldHostScreen extends WorldHostScreen {
    public final Screen parent;
    private Component status;
    private Connection connection;

    public JoiningWorldHostScreen(Screen parent) {
        super(GameNarrator.NO_TITLE);
        this.parent = parent;
        this.status = Components.translatable("world-host.joining_world_host");
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
        drawCenteredString(context, font, status, width / 2, height / 2 - 50, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        assert minecraft != null;
        minecraft.setScreen(parent);
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.setAttemptingToJoin(null);
        }
        if (connection != null) {
            connection.disconnect(Components.translatable("connect.aborted"));
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return connection == null;
    }

    @Override
    public void tick() {
        if (connection != null) {
            if (connection.isConnected()) {
                connection.tick();
            } else {
                connection.handleDisconnection();
            }
        }
    }

    public void setStatus(Component status) {
        this.status = status;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
        if (WorldHost.protoClient != null) {
            WorldHost.protoClient.setAttemptingToJoin(null);
        }
    }
}
