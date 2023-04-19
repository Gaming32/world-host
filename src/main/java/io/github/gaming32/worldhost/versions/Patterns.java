package io.github.gaming32.worldhost.versions;

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
}
