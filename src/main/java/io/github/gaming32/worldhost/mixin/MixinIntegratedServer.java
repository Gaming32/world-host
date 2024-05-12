package io.github.gaming32.worldhost.mixin;

import com.mojang.datafixers.DataFixer;
import io.github.gaming32.worldhost.proxy.ProxyChannels;
import io.github.gaming32.worldhost.proxy.ProxyClient;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

//#if MC > 1.18.2
import net.minecraft.server.Services;
//#else
//$$ import com.mojang.authlib.GameProfileRepository;
//$$ import com.mojang.authlib.minecraft.MinecraftSessionService;
//$$ import net.minecraft.Util;
//$$ import net.minecraft.network.chat.ChatType;
//$$ import net.minecraft.server.players.GameProfileCache;
//#endif

//#if MC < 1.18.2
//$$ import net.minecraft.core.RegistryAccess;
//$$ import net.minecraft.server.ServerResources;
//$$ import net.minecraft.world.level.storage.WorldData;
//#else
import net.minecraft.server.WorldStem;
//#endif

@Mixin(IntegratedServer.class)
public abstract class MixinIntegratedServer extends MinecraftServer {
    public MixinIntegratedServer(
        //#if MC > 1.18.2
        Thread thread,
        LevelStorageSource.LevelStorageAccess levelStorageAccess,
        PackRepository packRepository,
        WorldStem worldStem,
        Proxy proxy,
        DataFixer dataFixer,
        Services services,
        ChunkProgressListenerFactory chunkProgressListenerFactory
        //#elseif MC > 1.17.1
        //$$ Thread thread,
        //$$ LevelStorageSource.LevelStorageAccess levelStorageAccess,
        //$$ PackRepository packRepository,
        //$$ WorldStem worldStem,
        //$$ Proxy proxy,
        //$$ DataFixer dataFixer,
        //$$ MinecraftSessionService minecraftSessionService,
        //$$ GameProfileRepository gameProfileRepository,
        //$$ GameProfileCache gameProfileCache,
        //$$ ChunkProgressListenerFactory chunkProgressListenerFactory
        //#else
        //$$ Thread thread,
        //$$ RegistryAccess.RegistryHolder registryHolder,
        //$$ LevelStorageSource.LevelStorageAccess levelStorageAccess,
        //$$ WorldData worldData,
        //$$ PackRepository packRepository,
        //$$ Proxy proxy,
        //$$ DataFixer dataFixer,
        //$$ ServerResources serverResources,
        //$$ MinecraftSessionService minecraftSessionService,
        //$$ GameProfileRepository gameProfileRepository,
        //$$ GameProfileCache gameProfileCache,
        //$$ ChunkProgressListenerFactory chunkProgressListenerFactory
        //#endif
    ) {
        //#if MC > 1.18.2
        super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, services, chunkProgressListenerFactory);
        //#elseif MC > 1.17.1
        //$$ super(thread, levelStorageAccess, packRepository, worldStem, proxy, dataFixer, minecraftSessionService, gameProfileRepository, gameProfileCache, chunkProgressListenerFactory);
        //#else
        //$$ super(thread, registryHolder, levelStorageAccess, worldData, packRepository, proxy, dataFixer, serverResources, minecraftSessionService, gameProfileRepository, gameProfileCache, chunkProgressListenerFactory);
        //#endif
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
        if (!WorldHost.shareWorldOnLoad) return;
        WorldHost.shareWorldOnLoad = false;
        //#if MC < 1.20.5
        //$$ final boolean allowCommands = worldData.getAllowCommands();
        //#else
        final boolean allowCommands = worldData.isAllowCommands();
        //#endif
        final Component message;
        if (publishServer(worldData.getGameType(), allowCommands, HttpUtil.getAvailablePort())) {
            message = wh$getOpenedMessage();
        } else {
            message = Components.translatable("world-host.share_world.failed").withStyle(ChatFormatting.RED);
        }
        //#if MC > 1.18.2
        minecraft.getChatListener().handleSystemMessage(message, false);
        //#else
        //$$ minecraft.gui.handleChat(ChatType.SYSTEM, message, Util.NIL_UUID);
        //#endif
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

    @Inject(
        method = "publishServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerConnectionListener;startTcpServerListener(Ljava/net/InetAddress;I)V",
            shift = At.Shift.AFTER
        )
    )
    private void startProxyChannel(GameType gameMode, boolean cheats, int port, CallbackInfoReturnable<Boolean> cir) {
        WorldHost.proxySocketAddress = ProxyChannels.startProxyChannel(getConnection());
    }
}
