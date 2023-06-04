package io.github.gaming32.worldhost.mixin;

import com.mojang.brigadier.CommandDispatcher;
import io.github.gaming32.worldhost.WorldHost;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC > 1_18_02
import net.minecraft.commands.CommandBuildContext;
//#endif

@Mixin(Commands.class)
public class MixinCommands {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void commandRegistrationEvent(
        Commands.CommandSelection commandSelection,
        //#if MC > 1_18_02
        CommandBuildContext commandBuildContext,
        //#endif
        CallbackInfo ci
    ) {
        WorldHost.commandRegistrationHandler(dispatcher);
    }
}
