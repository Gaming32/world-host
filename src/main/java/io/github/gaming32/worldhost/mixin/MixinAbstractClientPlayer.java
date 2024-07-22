package io.github.gaming32.worldhost.mixin;

import com.mojang.authlib.GameProfile;
import io.github.gaming32.worldhost.gui.screen.PlayerInfoScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer extends Player {
    @Shadow @Nullable private PlayerInfo playerInfo;

    public MixinAbstractClientPlayer(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "getPlayerInfo", at = @At("HEAD"))
    private void fakeClientPlayerPlayerInfo(CallbackInfoReturnable<PlayerInfo> cir) {
        if (playerInfo == null && Minecraft.getInstance().getConnection() == null) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.getConnection() == null && minecraft.screen instanceof PlayerInfoScreen) {
                playerInfo = new PlayerInfo(getGameProfile(), false);
            }
        }
    }
}
