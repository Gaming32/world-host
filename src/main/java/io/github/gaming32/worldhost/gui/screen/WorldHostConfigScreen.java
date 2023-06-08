package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import io.github.gaming32.worldhost.gui.widget.EnumButton;
import io.github.gaming32.worldhost.gui.widget.YesNoButton;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

public class WorldHostConfigScreen extends WorldHostScreen {
    private static final Component TITLE = Components.translatable("world-host.config.title");
    private static final Component SERVER_IP = Components.translatable("world-host.config.serverIp");
    private static final Component ONLINE_STATUS_LOCATION = Components.translatable("world-host.config.onlineStatusLocation");
    private static final Component ENABLE_FRIENDS = Components.translatable("world-host.config.enableFriends");
    private static final Component ENABLE_RECONNECTION_TOASTS = Components.translatable("world-host.config.enableReconnectionToasts");
    private static final Component NO_UPNP = Components.translatable("world-host.config.noUPnP");
    private static final Component USE_SHORT_IP = Components.translatable("world-host.config.useShortIp");
    private static final Component SHOW_OUTDATED_WORLD_HOST = Components.translatable("world-host.config.showOutdatedWorldHost");

    private final Screen parent;

    private final String oldServerIp;
    private final boolean oldEnableFriends;
    private EditBox serverIpBox;

    public WorldHostConfigScreen(Screen parent) {
        super(TITLE);
        oldServerIp = WorldHost.CONFIG.getServerIp();
        oldEnableFriends = WorldHost.CONFIG.isEnableFriends();
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

        addRenderableWidget(new EnumButton<>(
            width / 2 + 5, yOffset + 24, 150, 20,
            "world-host.config.onlineStatusLocation",
            OnlineStatusLocation.class,
            button -> {
                WorldHost.CONFIG.setOnlineStatusLocation(button.getValue());
                WorldHost.saveConfig();
            }
        )).setValue(WorldHost.CONFIG.getOnlineStatusLocation());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 48, 150, 20,
            Components.translatable("world-host.config.enableFriends.tooltip"),
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

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 96, 150, 20,
            Components.translatable("world-host.config.noUPnP.tooltip"),
            button -> {
                WorldHost.CONFIG.setNoUPnP(button.isToggled());
                WorldHost.saveConfig();
                WorldHost.scanUpnp();
            }
        )).setToggled(WorldHost.CONFIG.isNoUPnP());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 120, 150, 20,
            Components.translatable("world-host.config.useShortIp.tooltip"),
            button -> {
                WorldHost.CONFIG.setUseShortIp(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isUseShortIp());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 144, 150, 20,
            button -> {
                WorldHost.CONFIG.setShowOutdatedWorldHost(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isShowOutdatedWorldHost());

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new FriendsScreen(this));
            }).pos(width / 2 - 155, yOffset + 168)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).pos(width / 2 + 5, yOffset + 168)
                .build()
        );
    }

    @Override
    public void resize(@NotNull Minecraft minecraft, int width, int height) {
        final String oldServerIp = serverIpBox.getValue();
        super.resize(minecraft, width, height);
        serverIpBox.setValue(oldServerIp);
    }

    @Override
    public void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, partialTick);
        drawCenteredString(context, font, title, width / 2, 15, 0xffffff);

        final int yOffset = height / 6 + 10 - font.lineHeight / 2;
        drawRightString(context, font, SERVER_IP, width / 2 - 5, yOffset, 0xffffff);
        drawRightString(context, font, ONLINE_STATUS_LOCATION, width / 2 - 5, yOffset + 24, 0xffffff);
        drawRightString(context, font, ENABLE_FRIENDS, width / 2 - 5, yOffset + 48, 0xffffff);
        drawRightString(context, font, ENABLE_RECONNECTION_TOASTS, width / 2 - 5, yOffset + 72, 0xffffff);
        drawRightString(context, font, NO_UPNP, width / 2 - 5, yOffset + 96, 0xffffff);
        drawRightString(context, font, USE_SHORT_IP, width / 2 - 5, yOffset + 120, 0xffffff);
        drawRightString(context, font, SHOW_OUTDATED_WORLD_HOST, width / 2 - 5, yOffset + 144, 0xffffff);
    }

    @Override
    public void removed() {
        if (!serverIpBox.getValue().equals(oldServerIp)) {
            WorldHost.CONFIG.setServerIp(serverIpBox.getValue());
            WorldHost.saveConfig();
            WorldHost.reconnect(true, true);
        }
        if (oldEnableFriends && !WorldHost.CONFIG.isEnableFriends() && WorldHost.protoClient != null) {
            WorldHost.protoClient.closedWorld(WorldHost.CONFIG.getFriends());
        }
    }

    @Override
    public void tick() {
        serverIpBox.tick();
    }
}
