package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 1.19.4
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import org.spongepowered.asm.mixin.injection.ModifyArgs;
//$$ import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
//#endif

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    //#if MC >= 1.19.4
    @Inject(method = "getSuccessMessage", at = @At("HEAD"), cancellable = true)
    private static void getSuccessMessage(int port, CallbackInfoReturnable<MutableComponent> cir) {
        if (WorldHost.CONFIG.isEnableFriends()) {
            cir.setReturnValue(Components.translatable(
                "world-host.lan_opened.friends",
                Components.copyOnClickText(port)
            ));
            return;
        }
        final String externalIp = WorldHost.getExternalIp();
        if (externalIp == null) return;
        cir.setReturnValue(Components.translatable(
            "world-host.lan_opened.no_friends",
            Components.copyOnClickText(externalIp),
            Components.copyOnClickText(port)
        ));
    }
    //#else
    //$$ @ModifyArgs(
    //$$     method = "publish",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target =
                //#if MC >= 1.19.2
                //$$ "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
                //#else
                //$$ "Lnet/minecraft/network/chat/TranslatableComponent;<init>(Ljava/lang/String;[Ljava/lang/Object;)V"
                //#endif
    //$$     )
    //$$ )
    //$$ private static void getSuccessMessage(Args args) {
    //$$     final Object[] tArgs = args.get(1);
    //$$     final Object port = tArgs[0];
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
    //$$         args.setAll(
    //$$             "world-host.lan_opened.friends",
    //$$             new Object[] {
    //$$                 Components.copyOnClickText(port)
    //$$             }
    //$$         );
    //$$         return;
    //$$     }
    //$$     final String externalIp = WorldHost.getExternalIp();
    //$$     if (externalIp == null) return;
    //$$     args.setAll(
    //$$         "world-host.lan_opened.no_friends",
    //$$         new Object[] {
    //$$             Components.copyOnClickText(externalIp),
    //$$             Components.copyOnClickText(port)
    //$$         }
    //$$     );
    //$$ }
    //#endif
}
