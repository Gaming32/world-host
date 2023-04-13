package io.github.gaming32.worldhost._1_19_2.gui;

import eu.midnightdust.lib.config.MidnightConfig;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostData;
import io.github.gaming32.worldhost.common.WorldHostTexts;
import io.github.gaming32.worldhost.common.gui.screen.FriendsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.MinecraftServer;

public class WorldHostConfigScreen extends MidnightConfig.MidnightConfigScreen {
    private final String oldServerUri = WorldHostData.serverUri;
    private final boolean oldEnableFriends = WorldHostData.enableFriends;

    public WorldHostConfigScreen(Screen parent) {
        super(parent, WorldHostCommon.MOD_ID);
    }

    @Override
    public void init() {
        super.init();

        if (!WorldHostData.enableFriends) return;

        final Button cancelButton = (Button)children().get(0);
        cancelButton.setWidth(80);
        cancelButton.x = width / 2 - 122;

        final Button doneButton = (Button)children().get(1);
        doneButton.setWidth(80);
        doneButton.x = width / 2 + 42;

        addRenderableWidget(new Button(width / 2 - 40, height - 28, 80, 20, WorldHostTexts.FRIENDS, button -> {
            assert minecraft != null;
            minecraft.setScreen(new FriendsScreen(this));
        }));
    }

    @Override
    public void removed() {
        if (!oldServerUri.equals(WorldHostData.serverUri)) {
            WorldHostCommon.reconnect(true, true);
        }
        if (oldEnableFriends && !WorldHostData.enableFriends && WorldHostCommon.wsClient != null) {
            final MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
            if (server != null && server.isPublished()) {
                WorldHostCommon.wsClient.closedWorld(WorldHostData.friends);
            }
        }
    }
}
