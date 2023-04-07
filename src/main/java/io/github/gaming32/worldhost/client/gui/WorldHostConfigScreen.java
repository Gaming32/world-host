package io.github.gaming32.worldhost.client.gui;

import eu.midnightdust.lib.config.MidnightConfig;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;

public class WorldHostConfigScreen extends MidnightConfig.MidnightConfigScreen {
    private final String oldServerUri = WorldHostData.serverUri;

    public WorldHostConfigScreen(Screen parent) {
        super(parent, WorldHost.MOD_ID);
    }

    @Override
    public void init() {
        super.init();

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
            WorldHostClient.reconnect(true, true);
        }
    }
}
