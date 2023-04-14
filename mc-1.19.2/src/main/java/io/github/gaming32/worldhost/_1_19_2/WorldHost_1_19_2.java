package io.github.gaming32.worldhost._1_19_2;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.gaming32.worldhost._1_19_2.mixin.client.MinecraftAccessor;
import io.github.gaming32.worldhost._1_19_2.mixin.client.ServerStatusPingerAccessor;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostPlatform;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import io.github.gaming32.worldhost.gui._1_19_2.WorldHostGui_1_19_2;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class WorldHost_1_19_2 implements WorldHostPlatform {
    private final WHGuiPlatform guiPlatform = new WorldHostGui_1_19_2();

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
        return new ClientboundStatusResponsePacket(buf).getStatus();
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
        return data.getIconB64();
    }

    @Override
    public void setIconData(ServerData data, @Nullable Object iconData) {
        data.setIconB64((String)iconData);
    }

    @Override
    public NativeImage readServerIcon(Object iconData) throws IOException {
        return NativeImage.fromBase64((String)iconData);
    }

    @Override
    public void updateServerInfo(ServerData serverInfo, ServerStatus metadata) {
        if (metadata.getDescription() != null) {
            serverInfo.motd = metadata.getDescription();
        } else {
            serverInfo.motd = CommonComponents.EMPTY;
        }

        if (metadata.getVersion() != null) {
            serverInfo.version = Component.literal(metadata.getVersion().getName());
            serverInfo.protocol = metadata.getVersion().getProtocol();
        } else {
            serverInfo.version = Component.translatable("multiplayer.status.old");
            serverInfo.protocol = 0;
        }

        serverInfo.playerList = List.of();
        if (metadata.getPlayers() != null) {
            serverInfo.status = ServerStatusPingerAccessor.formatPlayerCount(
                metadata.getPlayers().getNumPlayers(), metadata.getPlayers().getMaxPlayers()
            );
            final List<Component> lines = new ArrayList<>();
            final GameProfile[] sampleProfiles = metadata.getPlayers().getSample();
            if (sampleProfiles != null && sampleProfiles.length > 0) {
                for (final GameProfile sampleProfile : sampleProfiles) {
                    lines.add(Component.literal(sampleProfile.getName()));
                }
                if (sampleProfiles.length < metadata.getPlayers().getNumPlayers()) {
                    lines.add(Component.translatable(
                        "multiplayer.status.and_more", metadata.getPlayers().getNumPlayers() - sampleProfiles.length
                    ));
                }
                serverInfo.playerList = lines;
            }
        } else {
            serverInfo.status = Component.translatable("multiplayer.status.unknown").withStyle(ChatFormatting.DARK_GRAY);
        }

        String favicon = serverInfo.getIconB64();
        if (favicon != null) {
            try {
                favicon = ServerData.parseFavicon(favicon);
            } catch (ParseException e) {
                WorldHostCommon.LOGGER.error("Invalid server icon", e);
            }
        }

        serverInfo.setIconB64(favicon);
    }
}
