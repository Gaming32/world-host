package io.github.gaming32.worldhost.gui;

import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

//#if MC >= 11904
import net.minecraft.client.gui.components.Tooltip;
//#endif

public class YesNoButton extends Button {
    private static final Component YES = CommonComponents.GUI_YES.copy().withStyle(ChatFormatting.GREEN);
    private static final Component NO = CommonComponents.GUI_NO.copy().withStyle(ChatFormatting.RED);

    private final Consumer<YesNoButton> onToggle;

    private boolean toggled;

    public YesNoButton(int x, int y, int width, int height, Consumer<YesNoButton> onToggle) {
        this(x, y, width, height, null, onToggle);
    }

    public YesNoButton(int x, int y, int width, int height, @Nullable Component tooltip, Consumer<YesNoButton> onToggle) {
        super(
            x, y, width, height, Components.EMPTY, YesNoButton::onPress,
            //#if MC >= 11904
            DEFAULT_NARRATION
            //#else
            //$$ tooltip != null ? WorldHostScreen.onTooltip(tooltip) : NO_TOOLTIP
            //#endif
        );
        this.onToggle = onToggle;
        //#if MC >= 11904
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
        //#endif
    }

    private static void onPress(Button button) {
        final YesNoButton yesNoButton = (YesNoButton)button;
        yesNoButton.toggled = !yesNoButton.toggled;
        yesNoButton.onToggle.accept(yesNoButton);
    }

    public boolean isToggled() {
        return toggled;
    }

    public void setToggled(boolean toggled) {
        this.toggled = toggled;
    }

    @Override
    public void setMessage(@NotNull Component message) {
        throw new UnsupportedOperationException("Cannot set message of YesNoButton");
    }

    @NotNull
    @Override
    public Component getMessage() {
        return toggled ? YES : NO;
    }
}
