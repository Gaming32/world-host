package io.github.gaming32.worldhost.common.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public interface WHGuiPlatform {
    void sendRepeatEvents(boolean sendRepeatEvents);

    ButtonBuilder button(Component message, Button.OnPress onPress);

    void editBoxFocus(EditBox editBox, boolean focus);

    Button createFriendsButtonWidget(int x, int y, int width, int height, Button.OnPress onPress);

    boolean hasTooltips();

    void setTooltip(AbstractWidget widget, Component tooltip);

    void setY(AbstractWidget widget, int y);
}
