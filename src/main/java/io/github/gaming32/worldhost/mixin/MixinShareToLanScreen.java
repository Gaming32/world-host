package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC < 11904
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
//$$ import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
//$$ import io.github.gaming32.worldhost.versions.Components;
//$$ import net.minecraft.network.chat.MutableComponent;
//$$ import org.spongepowered.asm.mixin.injection.At;
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
    //$$ @WrapOperation(
        //#if MC < 11802
        //$$ method = "lambda$init$0",
        //#else
        //$$ method = "lambda$init$2",
        //#endif
    //$$     at = @At(
    //$$         value = "INVOKE",
            //#if MC >= 11902
            //$$ target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
            //#else
            //$$ target = "Lnet/minecraft/network/chat/TranslatableComponent;<init>(Ljava/lang/String;[Ljava/lang/Object;)V",
            //#endif
    //$$         ordinal = 0
    //$$     )
    //$$ )
    //$$ private
    //#if MC >= 11902
    //$$ MutableComponent
    //#else
    //$$ void
    //#endif
    //$$ changeSuccessMessage(
    //$$     String key,
    //$$     Object[] args,
    //$$     Operation<
            //#if MC >= 11902
            //$$ MutableComponent
            //#else
            //$$ Void
            //#endif
    //$$     > original
    //$$ ) {
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
            //#if MC >= 11902
            //$$ return
            //#endif
    //$$             original.call(
    //$$                 "world-host.lan_opened.friends",
    //$$                 new Object[] {Components.copyOnClickText(args[0])}
    //$$             );
    //$$     } else {
    //$$         final String externalIp = WorldHost.getExternalIp();
    //$$         if (externalIp == null) {
                //#if MC >= 11902
                //$$ return
                //#endif
    //$$                 original.call(key, args);
    //$$         } else {
                //#if MC >= 11902
                //$$ return
                //#endif
    //$$                 original.call(
    //$$                 "world-host.lan_opened.no_friends",
    //$$                     new Object[] {
    //$$                         Components.copyOnClickText(externalIp),
    //$$                         Components.copyOnClickText(args[0])
    //$$                     }
    //$$                 );
    //$$         }
    //$$     }
    //$$ }
    //#endif
}
