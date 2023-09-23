package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.WorldHostConfig;
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
    private static final Component UPNP = Components.literal("UPnP");

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

        final int serverAddressResetX = 145 - font.width(SERVER_IP);
        addRenderableWidget(
            button(Components.translatable("controls.reset"), b -> serverIpBox.setValue(WorldHostConfig.DEFAULT_SERVER_IP))
                .pos(width / 2 - serverAddressResetX, yOffset)
                .width(serverAddressResetX - 5)
                .build()
        );

        addRenderableWidget(new EnumButton<>(
            width / 2 - 155, yOffset + 24, 150, 20,
            "world-host.config.onlineStatusLocation",
            Components.translatable("world-host.config.onlineStatusLocation"),
            OnlineStatusLocation.class,
            button -> {
                WorldHost.CONFIG.setOnlineStatusLocation(button.getValue());
                WorldHost.saveConfig();
            }
        )).setValue(WorldHost.CONFIG.getOnlineStatusLocation());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 24, 150, 20,
            Components.translatable("world-host.config.enableFriends"),
            Components.translatable("world-host.config.enableFriends.tooltip"),
            button -> {
                WorldHost.CONFIG.setEnableFriends(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isEnableFriends());

        addRenderableWidget(new YesNoButton(
            width / 2 - 155, yOffset + 48, 150, 20,
            Components.translatable("world-host.config.enableReconnectionToasts"),
            button -> {
                WorldHost.CONFIG.setEnableReconnectionToasts(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isEnableReconnectionToasts());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 48, 150, 20,
            Components.translatable("world-host.config.noUPnP"),
            Components.translatable("world-host.config.noUPnP.tooltip"),
            button -> {
                WorldHost.CONFIG.setNoUPnP(button.isToggled());
                WorldHost.saveConfig();
                WorldHost.scanUpnp();
            }
        )).setToggled(WorldHost.CONFIG.isNoUPnP());

        addRenderableWidget(new YesNoButton(
            width / 2 - 155, yOffset + 72, 150, 20,
            Components.translatable("world-host.config.useShortIp"),
            Components.translatable("world-host.config.useShortIp.tooltip"),
            button -> {
                WorldHost.CONFIG.setUseShortIp(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isUseShortIp());

        addRenderableWidget(new YesNoButton(
            width / 2 + 5, yOffset + 72, 150, 20,
            Components.translatable("world-host.config.showOutdatedWorldHost"),
            button -> {
                WorldHost.CONFIG.setShowOutdatedWorldHost(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isShowOutdatedWorldHost());

        addRenderableWidget(new YesNoButton(
            width / 2 - 155, yOffset + 96, 150, 20,
            Components.translatable("world-host.config.shareButton"),
            Components.translatable("world-host.config.shareButton.tooltip"),
            button -> {
                WorldHost.CONFIG.setShareButton(button.isToggled());
                WorldHost.saveConfig();
            }
        )).setToggled(WorldHost.CONFIG.isShareButton());

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
        drawString(context, font, SERVER_IP, width / 2 - 150, yOffset, 0xffffff);

        drawRightString(context, font, UPNP, width - 7, height - 15, WorldHost.upnpGateway != null ? 0x55ff55 : 0xff5555);
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
