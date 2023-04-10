package io.github.gaming32.worldhost._1_19_4.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    /**
     * @author Gaming32
     * @reason We have a custom success message. What more do you want to know?
     */
    @Overwrite
    public static MutableComponent getSuccessMessage(int port) {
        return Component.translatable(
            "world-host.lan_opened.friends",
            ComponentUtils.copyOnClickText(Integer.toString(port))
        );
    }
}
