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

    public YesNoButton(int x, int y, int width, int height, Consumer<YesNoButton> onToggle) {
        super(x, y, width, height, onToggle, VALUES);
    }

    public YesNoButton(int x, int y, int width, int height, @Nullable Component tooltip, Consumer<YesNoButton> onToggle) {
        super(x, y, width, height, tooltip, onToggle, VALUES);
    }

    public boolean isToggled() {
        return getValue();
    }

    public void setToggled(boolean toggled) {
        setValueIndex(toggled ? 1 : 0);
    }

    @NotNull
    @Override
    public Component getMessage() {
        return isToggled() ? YES : NO;
    }
}
