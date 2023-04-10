package io.github.gaming32.worldhost._1_19_2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.gaming32.worldhost.common.Components;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostData;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    @WrapOperation(
        method = "publish",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
        )
    )
    private static MutableComponent changeSuccessMessage(String key, Object[] args, Operation<MutableComponent> original) {
        if (WorldHostData.enableFriends) {
            return original.call(
                "world-host.lan_opened.friends",
                new Object[] {Components.copyOnClickText(args[0])}
            );
        }
        final String externalIp = WorldHostCommon.getExternalIp();
        if (externalIp == null) {
            return original.call(key, args);
        }
        return original.call(
            "world-host.lan_opened.no_friends",
            new Object[] {
                Components.copyOnClickText(externalIp),
                Components.copyOnClickText(args[0])
            }
        );
    }
}
