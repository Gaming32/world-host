package io.github.gaming32.worldhost.gui._1_19_4;

import io.github.gaming32.worldhost.common.gui.ButtonBuilder;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class WorldHostGui_1_19_4 implements WHGuiPlatform {
    @Override
    public void sendRepeatEvents(boolean sendRepeatEvents) {
        // This isn't a thing in this version
    }

    @Override
    public ButtonBuilder button(Component message, Button.OnPress onPress) {
        return (ButtonBuilder)new Button.Builder(message, onPress);
    }

    @Override
    public void editBoxFocus(EditBox editBox, boolean focus) {
        editBox.setFocused(focus);
    }
}
