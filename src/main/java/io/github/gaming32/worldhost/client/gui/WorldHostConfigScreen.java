package io.github.gaming32.worldhost.client.gui;

import eu.midnightdust.lib.config.MidnightConfig;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public class WorldHostConfigScreen extends MidnightConfig.MidnightConfigScreen {
    private final String oldServerUri = WorldHostData.serverUri;

    public WorldHostConfigScreen(Screen parent) {
        super(parent, WorldHost.MOD_ID);
    }

    @Override
    public void init() {
        super.init();

        final ButtonWidget cancelButton = (ButtonWidget)children().get(0);
        cancelButton.setWidth(80);
        cancelButton.x = width / 2 - 122;

        final ButtonWidget doneButton = (ButtonWidget)children().get(1);
        doneButton.setWidth(80);
        doneButton.x = width / 2 + 42;

        addDrawableChild(new ButtonWidget(width / 2 - 40, height - 28, 80, 20, WorldHostTexts.FRIENDS, button -> {
            assert client != null;
            client.setScreen(new FriendsScreen(this));
        }));
    }

    @Override
    public void removed() {
        if (!oldServerUri.equals(WorldHostData.serverUri)) {
            WorldHostClient.reconnect(true, true);
        }
    }
}
