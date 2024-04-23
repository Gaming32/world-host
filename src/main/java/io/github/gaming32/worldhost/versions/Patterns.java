package io.github.gaming32.worldhost.versions;

import net.minecraft.client.User;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;

import java.util.UUID;

public class Patterns {
    @Pattern
    public static void setFocused(EditBox editBox, boolean focus) {
        //#if MC >= 1.19.4
        editBox.setFocused(focus);
        //#else
        //$$ editBox.setFocus(focus);
        //#endif
    }

    @Pattern
    public static int getY(AbstractWidget widget) {
        //#if MC >= 1.19.4
        return widget.getY();
        //#else
        //$$ return widget.y;
        //#endif
    }

    @Pattern
    public static UUID getProfileId(User user) {
        //#if MC >= 1.19.2
        return user.getProfileId();
        //#else
        //$$ return user.getGameProfile().getId();
        //#endif
    }
}
