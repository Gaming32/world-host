package io.github.gaming32.worldhost.versions;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

//#if MC >= 11904
import net.minecraft.client.gui.components.Tooltip;
//#else
//$$ import io.github.gaming32.worldhost.gui.GuiUtil;
//#endif

public class ButtonBuilder {
    //#if MC >= 11904
    private final Button.Builder builder;
    //#else
    //$$ private final Component message;
    //$$ private final Button.OnPress onPress;
    //$$
    //$$ private int x;
    //$$ private int y;
    //$$ private int width = 150;
    //$$ private int height = 20;
    //$$ private Button.OnTooltip onTooltip = Button.NO_TOOLTIP;
    //#endif

    public ButtonBuilder(Component message, Button.OnPress onPress) {
        //#if MC >= 11904
        builder = Button.builder(message, onPress);
        //#else
        //$$ this.message = message;
        //$$ this.onPress = onPress;
        //#endif
    }

    public ButtonBuilder pos(int x, int y) {
        //#if MC >= 11904
        builder.pos(x, y);
        //#else
        //$$ this.x = x;
        //$$ this.y = y;
        //#endif
        return this;
    }

    public ButtonBuilder width(int width) {
        //#if MC >= 11904
        builder.width(width);
        //#else
        //$$ this.width = width;
        //#endif
        return this;
    }

    public ButtonBuilder size(int width, int height) {
        //#if MC >= 11904
        builder.size(width, height);
        //#else
        //$$ this.width = width;
        //$$ this.height = height;
        //#endif
        return this;
    }

    public ButtonBuilder bounds(int x, int y, int width, int height) {
        //#if MC >= 11904
        builder.bounds(x, y, width, height);
        //#else
        //$$ pos(x, y).size(width, height);
        //#endif
        return this;
    }

    public ButtonBuilder tooltip(Component tooltip) {
        //#if MC >= 11904
        builder.tooltip(Tooltip.create(tooltip));
        //#else
        //$$ onTooltip = GuiUtil.onTooltip(tooltip);
        //#endif
        return this;
    }

    public Button build() {
        //#if MC >= 11904
        return builder.build();
        //#else
        //$$ return new Button(x, y, width, height, message, onPress, onTooltip);
        //#endif
    }
}