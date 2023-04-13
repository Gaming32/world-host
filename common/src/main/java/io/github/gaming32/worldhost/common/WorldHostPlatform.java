package io.github.gaming32.worldhost.common;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.gaming32.worldhost.common.gui.WHGuiPlatform;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface WorldHostPlatform {
    Services createServices();

    void showToast(ToastComponent toastComponent, SystemToast.SystemToastIds id, Component title, @Nullable Component message);

    ServerStatus parseServerStatus(FriendlyByteBuf buf);

    Screen createConfigScreen(Screen parent);

    WHGuiPlatform getGuiPlatform();

    MutableComponent translatableComponent(String key);

    MutableComponent translatableComponent(String key, Object... args);

    MutableComponent literalComponent(String text);

    Component immutableComponent(String text);

    @Nullable
    Object getIconData(ServerData data);

    void setIconData(ServerData data, @Nullable Object iconData);

    NativeImage readServerIcon(Object iconData) throws IOException;

    void updateServerInfo(ServerData serverInfo, ServerStatus metadata);
}
