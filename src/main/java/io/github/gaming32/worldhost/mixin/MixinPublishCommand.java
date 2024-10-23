package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.PublishCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

//#if MC >= 1.19.4
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//#else
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(PublishCommand.class)
public class MixinPublishCommand {
    //#if MC >= 1.19.4
    @Inject(method = "getSuccessMessage", at = @At("HEAD"), cancellable = true)
    private static void getSuccessMessage(int port, CallbackInfoReturnable<MutableComponent> cir) {
        if (WorldHost.CONFIG.isEnableFriends()) {
            cir.setReturnValue(Component.translatable(
                "world-host.lan_opened.friends",
                Components.copyOnClickText(port)
            ));
            return;
        }
        final String externalIp = WorldHost.getExternalIp();
        if (externalIp == null) return;
        cir.setReturnValue(Component.translatable(
            "world-host.lan_opened.no_friends",
            Components.copyOnClickText(externalIp),
            Components.copyOnClickText(port)
        ));
    }
    //#else
    //$$ @ModifyArg(
    //$$     method = "publish",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
    //$$     )
    //$$ )
    //$$ private static String getSuccessMessage(String key) {
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
    //$$         return "world-host.lan_opened.friends";
    //$$     }
    //$$     final String externalIp = WorldHost.getExternalIp();
    //$$     if (externalIp == null) {
    //$$         return key;
    //$$     }
    //$$     return "world-host.lan_opened.no_friends";
    //$$ }
    //$$
    //$$ @ModifyArg(
    //$$     method = "publish",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
    //$$     )
    //$$ )
    //$$ private static Object[] getSuccessMessage(Object[] args) {
    //$$     final Object port = args[0];
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
    //$$         return new Object[] {
    //$$             Components.copyOnClickText(port)
    //$$         };
    //$$     }
    //$$     final String externalIp = WorldHost.getExternalIp();
    //$$     if (externalIp == null) {
    //$$         return args;
    //$$     }
    //$$     return new Object[] {
    //$$         Components.copyOnClickText(externalIp),
    //$$         Components.copyOnClickText(port)
    //$$     };
    //$$ }
    //#endif
}
