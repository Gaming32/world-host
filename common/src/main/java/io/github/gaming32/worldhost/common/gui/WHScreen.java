package io.github.gaming32.worldhost.common.gui;

import io.github.gaming32.worldhost.common.WorldHostCommon;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class WHScreen extends Screen {
    protected final WHGuiPlatform platform = WorldHostCommon.getPlatform().getGuiPlatform();

    protected WHScreen(Component component) {
        super(component);
    }

    protected void sendRepeatEvents(boolean sendRepeatEvents) {
        platform.sendRepeatEvents(sendRepeatEvents);
    }

    protected ButtonBuilder button(Component message, Button.OnPress onPress) {
        return platform.button(message, onPress);
    }

    protected void editBoxFocus(EditBox editBox, boolean focus) {
        platform.editBoxFocus(editBox, focus);
    }
}
