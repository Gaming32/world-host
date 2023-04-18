package io.github.gaming32.worldhost.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import static io.github.gaming32.worldhost.gui.GuiUtil.drawRightString;

public class WorldHostConfigScreen extends Screen {
    private static final Component TITLE = Components.translatable("world-host.config.title");
    private static final Component SERVER_IP = Components.translatable("world-host.config.serverIp");
    private static final Component SHOW_ONLINE_STATUS = Components.translatable("world-host.config.showOnlineStatus");
    private static final Component ENABLE_FRIENDS = Components.translatable("world-host.config.enableFriends");
    private static final Component ENABLE_RECONNECTION_TOASTS = Components.translatable("world-host.config.enableReconnectionToasts");

    private final Screen parent;

    private final String oldServerIp;
    private EditBox serverIpBox;

    public WorldHostConfigScreen(Screen parent) {
        super(TITLE);
        oldServerIp = WorldHost.CONFIG.getServerIp();
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        final int yOffset = height / 6;

        serverIpBox = addRenderableWidget(new EditBox(
            font, width / 2 + 5, yOffset, 150, 20, SERVER_IP
        ));
        serverIpBox.setValue(WorldHost.CONFIG.getServerIp());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 24, 150, 20,
            button -> {
                WorldHost.CONFIG.setShowOnlineStatus(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isShowOnlineStatus());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 48, 150, 20,
            button -> {
                WorldHost.CONFIG.setEnableFriends(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isEnableFriends());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 72, 150, 20,
            button -> {
                WorldHost.CONFIG.setEnableReconnectionToasts(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isEnableReconnectionToasts());
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        final String oldServerIp = serverIpBox.getValue();
        super.resize(minecraft, width, height);
        serverIpBox.setValue(oldServerIp);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, 15, 0xffffff);

        final int yOffset = height / 6 + 10 - font.lineHeight / 2;
        drawRightString(poseStack, font, SERVER_IP, width / 2 - 5, yOffset, 0xffffff);
        drawRightString(poseStack, font, SHOW_ONLINE_STATUS, width / 2 - 5, yOffset + 24, 0xffffff);
        drawRightString(poseStack, font, ENABLE_FRIENDS, width / 2 - 5, yOffset + 48, 0xffffff);
        drawRightString(poseStack, font, ENABLE_RECONNECTION_TOASTS, width / 2 - 5, yOffset + 72, 0xffffff);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (!serverIpBox.getValue().equals(oldServerIp)) {
            WorldHost.CONFIG.setServerIp(serverIpBox.getValue());
            WorldHost.saveConfig();
            // TODO: Perform reconnect
        }
    }
}
