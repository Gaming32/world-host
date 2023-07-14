package io.github.gaming32.worldhost.mixin;

import com.mojang.datafixers.DataFixer;
import io.github.gaming32.worldhost.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.HttpUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.Proxy;
import java.util.UUID;

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer {
    public MixinIntegratedServer(Thread thread, LevelStorageSource.LevelStorageAccess levelStorageAccess, PackRepository packRepository, WorldStem worldStem, Proxy proxy, DataFixer dataFixer, Services services, ChunkProgressListenerFactory chunkProgressListenerFactory) {
        super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, services, chunkProgressListenerFactory);
    }

    @Shadow @Final private Minecraft minecraft;

    @Shadow private int publishedPort;

    @Inject(method = "publishServer", at = @At(value = "RETURN", ordinal = 0))
    private void serverIsOpen(GameType gameMode, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        if (WorldHost.protoClient != null && WorldHost.CONFIG.isEnableFriends()) {
            WorldHost.protoClient.publishedWorld(WorldHost.CONFIG.getFriends());
        }
    }

    @Inject(method = "halt", at = @At("TAIL"))
    private void serverIsClosed(boolean waitForServer, CallbackInfo ci) {
        WorldHost.CONNECTED_PROXY_CLIENTS.values().forEach(ProxyClient::close);
        WorldHost.CONNECTED_PROXY_CLIENTS.clear();
        if (isPublished() && WorldHost.protoClient != null) {
            WorldHost.protoClient.closedWorld(WorldHost.CONFIG.getFriends());
        }
    }

    @Inject(method = "setUUID", at = @At("TAIL"))
    private void shareWorldOnLoad(UUID uuid, CallbackInfo ci) {
        if (!WorldHost.shareWorldOnLoadReal) return;
        WorldHost.shareWorldOnLoadReal = false;
        if (publishServer(worldData.getGameType(), worldData.getAllowCommands(), HttpUtil.getAvailablePort())) {
            minecraft.getChatListener().handleSystemMessage(wh$getOpenedMessage(), false);
        } else {
            minecraft.getChatListener().handleSystemMessage(
                Components.translatable("world-host.share_world.failed").withStyle(ChatFormatting.RED), false
            );
        }
    }

    @Unique
    private Component wh$getOpenedMessage() {
        final Component port = Components.copyOnClickText(publishedPort);
        if (WorldHost.CONFIG.isEnableFriends()) {
            return Components.translatable("world-host.lan_opened.friends", port);
        }
        final String externalIp = WorldHost.getExternalIp();
        if (externalIp == null) {
            return Components.translatable("commands.publish.started", port);
        }
        return Components.translatable(
            "world-host.lan_opened.no_friends",
            Components.copyOnClickText(externalIp), port
        );
    }
}
