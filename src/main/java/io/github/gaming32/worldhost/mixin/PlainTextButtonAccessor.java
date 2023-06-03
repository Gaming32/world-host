package io.github.gaming32.worldhost.mixin;

import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

//#if MC > 11701
import net.minecraft.client.gui.components.PlainTextButton;
//#else
//$$ import io.github.gaming32.worldhost.gui.PlainTextButton;
//#endif

@Mixin(PlainTextButton.class)
public interface PlainTextButtonAccessor {
    @Mutable
    @Accessor("message")
    void setPTBMessage(Component message);

    @Mutable
    @Accessor("underlinedMessage")
    void setUnderlinedMessage(Component underlinedMessage);
}
