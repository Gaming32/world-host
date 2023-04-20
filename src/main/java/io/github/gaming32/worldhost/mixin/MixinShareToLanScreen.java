package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

//#if MC < 11904
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
    //$$     method = "method_19851",
    //$$     at = @At(
    //$$         value = "INVOKE",
    //$$         target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
    //$$         ordinal = 0
    //$$     )
    //$$ )
    //$$ private MutableComponent changeSuccessMessage(String key, Object[] args, Operation<MutableComponent> original) {
    //$$     if (WorldHost.CONFIG.isEnableFriends()) {
    //$$         return original.call(
    //$$             "world-host.lan_opened.friends",
    //$$             new Object[] {Components.copyOnClickText(args[0])}
    //$$         );
    //$$     }
    //$$     final String externalIp = WorldHost.getExternalIp();
    //$$     if (externalIp == null) {
    //$$         return original.call(key, args);
    //$$     }
    //$$     return original.call(
    //$$         "world-host.lan_opened.no_friends",
    //$$         new Object[] {
    //$$             Components.copyOnClickText(externalIp),
    //$$             Components.copyOnClickText(args[0])
    //$$         }
    //$$     );
    //$$ }
    //#endif
}
