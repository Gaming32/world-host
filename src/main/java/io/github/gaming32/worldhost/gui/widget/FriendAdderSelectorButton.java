package io.github.gaming32.worldhost.gui.widget;

import io.github.gaming32.worldhost.plugin.FriendAdder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class FriendAdderSelectorButton extends CustomCycleButton<FriendAdder, FriendAdderSelectorButton> {
    public FriendAdderSelectorButton(
        int x, int y, int width, int height,
        @Nullable Component title,
        Consumer<FriendAdderSelectorButton> onUpdate,
        FriendAdder[] values
    ) {
        super(x, y, width, height, title, onUpdate, values);
    }

    @Override
    public @NotNull Component getValueMessage() {
        return getValue().label();
    }
}
