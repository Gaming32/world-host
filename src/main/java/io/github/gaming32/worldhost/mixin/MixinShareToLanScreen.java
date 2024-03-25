package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC < 1.19.4
//$$ import io.github.gaming32.worldhost.versions.Components;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArg;
//#endif

@Mixin(ShareToLanScreen.class)
public class MixinShareToLanScreen {
    @ModifyConstant(method = "<init>*", constant = @Constant(stringValue = "lanServer.title"))
    private static String changeLabelI1(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    @ModifyConstant(method = "init()V", constant = @Constant(stringValue = "lanServer.start"))
    private String changeLabelI2(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    //#if MC < 1.19.4
    //$$ @ModifyArg(
    //$$     method =
            //#if MC < 1.17.1
            //$$ {"lambda$init$0*", "func_213082_d"}, // Mixin can't find lambda$init$0 for some reason, so have an obfuscated method name :)
            //#else
            //$$ "lambda$init$2",
            //#endif
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
    //$$ private String changeSuccessMessage(String key) {
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
    //$$         return "world-host.lan_opened.friends";
    //$$     }
    //$$     final String externalIp = WorldHost.getExternalIp();
    //$$     return externalIp != null ? "world-host.lan_opened.no_friends" : key;
    //$$ }
    //$$
    //$$ @ModifyArg(
    //$$     method =
            //#if MC < 1.17.1
            //#if FABRIC
            //$$ "lambda$init$0",
            //#else
            //$$ {"lambda$init$0*", "func_213082_d"}, // Mixin can't find lambda$init$0 for some reason, so have an obfuscated method name :)
            //#endif
            //#else
            //$$ "lambda$init$2",
            //#endif
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
    //$$ private Object[] changeSuccessMessage(Object[] args) {
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
