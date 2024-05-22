package io.github.gaming32.worldhost.gui.screen;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostComponents;
import io.github.gaming32.worldhost.config.WorldHostConfig;
import io.github.gaming32.worldhost.config.option.ConfigOptions;
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

        int yOffset = height / 6;

        serverIpBox = addRenderableWidget(new EditBox(font, width / 2 + 5, yOffset, 150, 20, SERVER_IP));
        serverIpBox.setValue(WorldHost.CONFIG.getServerIp());

        final int serverAddressResetX = 145 - font.width(SERVER_IP);
        addRenderableWidget(
            button(Components.translatable("controls.reset"), b -> serverIpBox.setValue(WorldHostConfig.DEFAULT_SERVER_IP))
                .pos(width / 2 - serverAddressResetX, yOffset)
                .width(serverAddressResetX - 5)
                .build()
        );

        int optionIndex = 0;
        for (final var option : ConfigOptions.OPTIONS.values()) {
            if ((optionIndex & 1) == 0) {
                yOffset += 24;
            }
            addRenderableWidget(option.createButton(width / 2 - 155 + 160 * (optionIndex % 2), yOffset, 150, 20));
            optionIndex++;
        }
        yOffset += 48;

        addRenderableWidget(
            button(WorldHostComponents.FRIENDS, button -> {
                assert minecraft != null;
                minecraft.setScreen(new FriendsScreen(this));
            }).pos(width / 2 - 155, yOffset)
                .build()
        );

        addRenderableWidget(
            button(CommonComponents.GUI_DONE, button -> {
                assert minecraft != null;
                minecraft.setScreen(parent);
            }).pos(width / 2 + 5, yOffset)
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
        int mouseX, int mouseY, float delta
    ) {
        whRenderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
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

    //#if MC < 1.20.2
    //$$ @Override
    //$$ public void tick() {
    //$$     serverIpBox.tick();
    //$$ }
    //#endif
}
