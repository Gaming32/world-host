package io.github.gaming32.worldhost.gui._1_19_4;

import io.github.gaming32.worldhost.common.gui.ButtonBuilder;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class WorldHostGui_1_19_4 implements WHGuiPlatform {
    @Override
    public void sendRepeatEvents(boolean sendRepeatEvents) {
    }

    @Override
    public ButtonBuilder button(Component message, Button.OnPress onPress) {
        return (ButtonBuilder)new Button.Builder(message, onPress);
    }

    @Override
    public void editBoxFocus(EditBox editBox, boolean focus) {
        editBox.setFocused(focus);
    }

    @Override
    public Button createFriendsButtonWidget(int x, int y, int width, int height, Button.OnPress onPress) {
        return new FriendsButtonWidget(x, y, width, height, onPress);
    }

    @Override
    public boolean hasTooltips() {
        return true;
    }

    @Override
    public void setTooltip(AbstractWidget widget, Component tooltip) {
        widget.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void setY(AbstractWidget widget, int y) {
        widget.setY(y);
    }
}
