package io.github.gaming32.worldhost.gui._1_19_2;

import io.github.gaming32.worldhost.common.gui.ButtonBuilder;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class WorldHostGui_1_19_2 implements WHGuiPlatform {
    @Override
    public void sendRepeatEvents(boolean sendRepeatEvents) {
        Minecraft.getInstance().keyboardHandler.setSendRepeatsToGui(sendRepeatEvents);
    }

    @Override
    public ButtonBuilder button(Component message, Button.OnPress onPress) {
        return new ButtonBuilder() {
            private int x;
            private int y;
            private int width = 150;
            private int height = 20;

            @Override
            public ButtonBuilder pos(int x, int y) {
                this.x = x;
                this.y = y;
                return this;
            }

            @Override
            public ButtonBuilder width(int width) {
                this.width = width;
                return this;
            }

            @Override
            public ButtonBuilder size(int width, int height) {
                this.width = width;
                this.height = height;
                return this;
            }

            @Override
            public ButtonBuilder bounds(int x, int y, int width, int height) {
                return this.pos(x, y).size(width, height);
            }

            @Override
            public ButtonBuilder tooltip(Component tooltip) {
                // Tooltips aren't supported in this version
                return this;
            }

            @Override
            public Button build() {
                return new Button(x, y, width, height, message, onPress);
            }
        };
    }

    @Override
    public void editBoxFocus(EditBox editBox, boolean focus) {
        editBox.setFocus(focus);
    }

    @Override
    public Button createFriendsButtonWidget(int x, int y, int width, int height, Button.OnPress onPress) {
        return new FriendsButtonWidget(x, y, width, height, onPress);
    }
}
