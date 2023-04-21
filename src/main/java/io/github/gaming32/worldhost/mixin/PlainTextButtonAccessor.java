package io.github.gaming32.worldhost.mixin;

import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlainTextButton.class)
public interface PlainTextButtonAccessor {
    @Mutable
    @Accessor("message")
    void setPTBMessage(Component message);

    @Mutable
    @Accessor("underlinedMessage")
    void setUnderlinedMessage(Component underlinedMessage);
}
