package io.github.gaming32.worldhost.versions;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;

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
}
