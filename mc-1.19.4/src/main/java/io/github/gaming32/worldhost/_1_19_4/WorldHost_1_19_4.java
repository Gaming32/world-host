package io.github.gaming32.worldhost._1_19_4;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.gaming32.worldhost._1_19_4.mixin.client.MinecraftAccessor;
import io.github.gaming32.worldhost._1_19_4.mixin.client.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostPlatform;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import io.github.gaming32.worldhost.gui._1_19_4.WorldHostGui_1_19_4;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorldHost_1_19_4 implements WorldHostPlatform {
    private final WHGuiPlatform guiPlatform = new WorldHostGui_1_19_4();

    @Override
    public Services createServices() {
        return Services.create(
            ((MinecraftAccessor)Minecraft.getInstance()).getAuthenticationService(),
            WorldHostCommon.CACHE_DIR
        );
    }

    @Override
    public void showToast(ToastComponent toastComponent, SystemToast.SystemToastIds id, Component title, @Nullable Component message) {
        SystemToast.addOrUpdate(toastComponent, id, title, message);
    }

    @Override
    public ServerStatus parseServerStatus(FriendlyByteBuf buf) {
        return new ClientboundStatusResponsePacket(buf).status();
    }

    @Override
    public WHGuiPlatform getGuiPlatform() {
        return guiPlatform;
    }

    @Override
    public MutableComponent emptyComponent() {
        return Component.empty();
    }

    @Override
    public MutableComponent translatableComponent(String key) {
        return Component.translatable(key);
    }

    @Override
    public MutableComponent translatableComponent(String key, Object... args) {
        return Component.translatable(key, args);
    }

    @Override
    public MutableComponent literalComponent(String text) {
        return Component.literal(text);
    }

    @Override
    public Component immutableComponent(String text) {
        return Component.nullToEmpty(text);
    }

    @Override
    @Nullable
    public Object getIconData(ServerData data) {
        return data.getIconBytes();
    }

    @Override
    public void setIconData(ServerData data, @Nullable Object iconData) {
        data.setIconBytes((byte[])iconData);
    }

    @Override
    public NativeImage readServerIcon(Object iconData) throws IOException {
        return NativeImage.read((byte[])iconData);
    }

    @Override
    public void updateServerInfo(ServerData serverInfo, ServerStatus metadata) {
        serverInfo.motd = metadata.description();
        metadata.version().ifPresentOrElse(version -> {
            serverInfo.version = Component.literal(version.name());
            serverInfo.protocol = version.protocol();
        }, () -> {
            serverInfo.version = Component.translatable("multiplayer.status.old");
            serverInfo.protocol = 0;
        });
        metadata.players().ifPresentOrElse(players -> {
            serverInfo.status = ServerStatusPingerAccessor.formatPlayerCount(players.online(), players.max());
            serverInfo.players = players;
            if (!players.sample().isEmpty()) {
                final List<Component> playerList = new ArrayList<>(players.sample().size());

                for(GameProfile gameProfile : players.sample()) {
                    playerList.add(Component.literal(gameProfile.getName()));
                }

                if (players.sample().size() < players.online()) {
                    playerList.add(Component.translatable(
                        "multiplayer.status.and_more",
                        players.online() - players.sample().size()
                    ));
                }

                serverInfo.playerList = playerList;
            } else {
                serverInfo.playerList = List.of();
            }
        }, () -> serverInfo.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY));
        metadata.favicon().ifPresent(favicon -> {
            if (!Arrays.equals(favicon.iconBytes(), serverInfo.getIconBytes())) {
                serverInfo.setIconBytes(favicon.iconBytes());
            }
        });
    }
}
