package io.github.gaming32.worldhost.common.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public interface ButtonBuilder {
    ButtonBuilder pos(int x, int y);

    ButtonBuilder width(int width);

    ButtonBuilder size(int width, int height);

    ButtonBuilder bounds(int x, int y, int width, int height);

    ButtonBuilder tooltip(Component tooltip);

    Button build();
}
