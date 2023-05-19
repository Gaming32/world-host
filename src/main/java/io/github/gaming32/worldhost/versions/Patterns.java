package io.github.gaming32.worldhost.versions;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.List;

//#if MC >= 11605
import net.minecraft.util.FormattedCharSequence;
//#else
//$$ import net.minecraft.network.chat.FormattedText;
//#endif

public class Patterns {
    @Pattern
    public static void setFocused(EditBox editBox, boolean focus) {
        //#if MC >= 11904
        editBox.setFocused(focus);
        //#else
        //$$ editBox.setFocus(focus);
        //#endif
    }

    @Pattern
    public static int getY(AbstractWidget widget) {
        //#if MC >= 11904
        return widget.getY();
        //#else
        //$$ return widget.y;
        //#endif
    }

    @Pattern
    public static List<
        //#if MC >= 11605
        FormattedCharSequence
        //#else
        //$$ FormattedText
        //#endif
    > split(Font font, Component text, int width) {
        //#if MC >= 11605
        return font.split(text, width);
        //#else
        //$$ return font.getSplitter().splitLines(text, width, net.minecraft.network.chat.Style.EMPTY);
        //#endif
    }
}
