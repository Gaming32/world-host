package io.github.gaming32.worldhost._1_19_4.mixin;

import io.github.gaming32.worldhost.common.Components;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    @Inject(method = "getSuccessMessage", at = @At("HEAD"), cancellable = true)
    private static void getSuccessMessage(int port, CallbackInfoReturnable<MutableComponent> cir) {
        if (WorldHostData.enableFriends) {
            cir.setReturnValue(Component.translatable(
                "world-host.lan_opened.friends",
                Components.copyOnClickText(port)
            ));
            return;
        }
        final String externalIp = WorldHostCommon.getExternalIp();
        if (externalIp == null) return;
        cir.setReturnValue(Component.translatable(
            "world-host.lan_opened.no_friends",
            Components.copyOnClickText(externalIp),
            Components.copyOnClickText(port)
        ));
    }
}
