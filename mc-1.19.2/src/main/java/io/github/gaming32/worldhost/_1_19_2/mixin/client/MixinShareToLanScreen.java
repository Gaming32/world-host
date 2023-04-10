package io.github.gaming32.worldhost._1_19_2.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.gaming32.worldhost.common.Components;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostData;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ShareToLanScreen.class)
public class MixinShareToLanScreen {
    @ModifyConstant(method = "<init>", constant = @Constant(stringValue = "lanServer.title"))
    private static String changeLabelI1(String constant) {
        return WorldHostData.enableFriends ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    @ModifyConstant(method = "init", constant = @Constant(stringValue = "lanServer.start"))
    private String changeLabelI2(String constant) {
        return WorldHostData.enableFriends ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    @WrapOperation(
        method = "method_19851",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
            ordinal = 0
        )
    )
    private MutableComponent changeSuccessMessage(String key, Object[] args, Operation<MutableComponent> original) {
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
