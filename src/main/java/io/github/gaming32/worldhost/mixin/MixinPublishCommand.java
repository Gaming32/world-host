package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 11904
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//#endif

//#if MC >= 11902
import net.minecraft.network.chat.MutableComponent;
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
            //#if MC >= 11902
            //$$ target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            //#else
            //$$ target = "Lnet/minecraft/network/chat/TranslatableComponent;<init>(Ljava/lang/String;[Ljava/lang/Object;)V"
            //#endif
    //$$     )
    //$$ )
    //#endif
    private static
    //#if MC >= 11902 && MC < 11904
    //$$ MutableComponent
    //#else
    void
    //#endif
    getSuccessMessage(
        //#if MC < 11902
        //$$ TranslatableComponent component,
        //#endif
        //#if MC >= 11904
        int port, CallbackInfoReturnable<MutableComponent> cir
        //#else
        //$$ String key, Object[] args,
        //$$ Operation<
            //#if MC >= 11902
            //$$ MutableComponent
            //#else
            //$$ Void
            //#endif
        //$$ > original
        //#endif
    ) {
        //#if MC < 11904
        //$$ final Object port = args[0];
        //#endif
        if (WorldHost.CONFIG.isEnableFriends()) {
            //#if MC >= 11904
            cir.setReturnValue(Components.translatable(
            //#else
            //#if MC >= 11902
            //$$ return
            //#endif
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
            //#if MC < 11902
            //$$ return;
            //#endif
        }
        final String externalIp = WorldHost.getExternalIp();
        if (externalIp == null) {
            return
                //#if MC >= 11902 && MC < 11904
                //$$ original.call(key, args)
                //#endif
                ;
        }
        //#if MC >= 11904
        cir.setReturnValue(Components.translatable(
        //#else
        //#if MC >= 11902
        //$$ return
        //#endif
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
