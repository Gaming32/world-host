package io.github.gaming32.worldhost.gui.widget;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class YesNoButton extends CustomCycleButton<Boolean, YesNoButton> {
    private static final Boolean[] VALUES = {false, true};
    private static final Component YES = CommonComponents.GUI_YES.copy().withStyle(ChatFormatting.GREEN);
    private static final Component NO = CommonComponents.GUI_NO.copy().withStyle(ChatFormatting.RED);

    public YesNoButton(
        int x, int y,
        int width, int height,
        @Nullable Component title,
        Consumer<YesNoButton> onToggle
    ) {
        super(x, y, width, height, title, onToggle, VALUES);
    }

    public YesNoButton(
        int x, int y,
        int width, int height,
        @Nullable Component title, @Nullable Component tooltip,
        Consumer<YesNoButton> onToggle
    ) {
        super(x, y, width, height, title, tooltip, onToggle, VALUES);
    }

    public boolean isToggled() {
        return getValue();
    }

    public void setToggled(boolean toggled) {
        setValueIndex(toggled ? 1 : 0);
    }

    @NotNull
    @Override
    public Component getValueMessage() {
        return isToggled() ? YES : NO;
    }
}
