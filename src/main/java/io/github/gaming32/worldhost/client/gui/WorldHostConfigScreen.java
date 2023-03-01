package io.github.gaming32.worldhost.client.gui;

import eu.midnightdust.lib.config.MidnightConfig;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.WorldHostData;
import io.github.gaming32.worldhost.WorldHostTexts;
import io.github.gaming32.worldhost.client.WorldHostClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;

public class WorldHostConfigScreen extends MidnightConfig.MidnightConfigScreen {
    private final String oldServerUri = WorldHostData.serverUri;

    public WorldHostConfigScreen(Screen parent) {
        super(parent, WorldHost.MOD_ID);
    }

    @Override
    public void init() {
        super.init();
        ((ClickableWidget)children().get(0)).x = width / 2 - 229;
        ((ClickableWidget)children().get(1)).x = width / 2 - 75;
        addDrawableChild(new ButtonWidget(width / 2 + 79, height - 28, 150, 20, WorldHostTexts.FRIENDS, button -> {
            assert client != null;
            client.setScreen(new FriendsScreen(this));
        }));
    }

    @Override
    public void removed() {
        if (!oldServerUri.equals(WorldHostData.serverUri)) {
            WorldHostClient.reconnect(true);
        }
    }
}
