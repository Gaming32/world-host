package io.github.gaming32.worldhost.mixin;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC >= 1.21
import net.minecraft.network.DisconnectionDetails;
//#else
//$$ import net.minecraft.network.chat.Component;
//#endif

@Mixin(DisconnectedScreen.class)
public interface DisconnectedScreenAccessor {
    @Accessor
    //#if MC >= 1.21
    DisconnectionDetails getDetails();
    //#else
    //$$ Component getReason();
    //#endif
}
