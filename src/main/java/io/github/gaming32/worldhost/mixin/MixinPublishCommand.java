package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//#if MC < 11904
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#endif

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    //#if MC >= 11904
    @Inject(
        method = "getSuccessMessage",
        at = @At("HEAD"),
        cancellable = true
    )
    @SuppressWarnings("RedundantArrayCreation")
    //#else
    //$$ @WrapOperation(
    //$$     method = "publish",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
    //$$     )
    //$$ )
    //#endif
    private static void getSuccessMessage(
        //#if MC >= 11904
        int port, CallbackInfoReturnable<MutableComponent> cir
        //#else
        //$$ String key, Object[] args, Operation<MutableComponent> original
        //#endif
    ) {
        //#if MC < 11904
        //$$ final Object port = args[0];
        //#endif
        if (WorldHost.CONFIG.isEnableFriends()) {
            //#if MC >= 11904
            cir.setReturnValue(Components.translatable(
            //#else
            //$$ original.call(
            //#endif
                "world-host.lan_opened.friends",
                new Object[] {
                    Components.copyOnClickText(port)
                }
                //#if MC >= 11904
                )
                //#endif
            );
            return;
        }
        final String externalIp = WorldHost.getExternalIp();
        if (externalIp == null) return;
        //#if MC >= 11904
        cir.setReturnValue(Components.translatable(
        //#else
        //$$ original.call(
        //#endif
            "world-host.lan_opened.no_friends",
            new Object[] {
                Components.copyOnClickText(externalIp),
                Components.copyOnClickText(port)
            }
            //#if MC >= 11904
            )
            //#endif
        );
    }
}
