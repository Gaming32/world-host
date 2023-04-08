package io.github.gaming32.worldhost.common;

import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

public interface WorldHostPlatform {
    Services createServices();

    void showToast(ToastComponent toastComponent, SystemToast.SystemToastIds id, Component title, @Nullable Component message);

    ServerStatus parseServerStatus(FriendlyByteBuf buf);

    MutableComponent translatableComponent(String key);

    MutableComponent translatableComponent(String key, Object... args);

    Component immutableComponent(String text);
}
