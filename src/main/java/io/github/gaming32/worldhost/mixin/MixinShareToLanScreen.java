package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC < 11904
//$$ import io.github.gaming32.worldhost.versions.Components;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.ModifyArgs;
//$$ import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
//#endif

@Mixin(ShareToLanScreen.class)
public class MixinShareToLanScreen {
    @ModifyConstant(method = "<init>", constant = @Constant(stringValue = "lanServer.title"))
    private static String changeLabelI1(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    @ModifyConstant(method = "init", constant = @Constant(stringValue = "lanServer.start"))
    private String changeLabelI2(String constant) {
        return WorldHost.CONFIG.isEnableFriends() ? "world-host.open_world" : "world-host.open_world_no_friends";
    }

    //#if MC < 11904
    //$$ @ModifyArgs(
    //$$     method =
            //#if MC < 11802
            //#if FABRIC
            //$$ "lambda$init$0",
            //#else
            //$$ "func_213082_d", // Mixin can't find lambda$init$0 for some reason, so have an obfuscated method name :)
            //#endif
            //#else
            //$$ "lambda$init$2",
            //#endif
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target =
                //#if MC >= 11902
                //$$ "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
                //#else
                //$$ "Lnet/minecraft/network/chat/TranslatableComponent;<init>(Ljava/lang/String;[Ljava/lang/Object;)V"
                //#endif
    //$$     )
    //$$ )
    //$$ private void changeSuccessMessage(Args args) {
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
