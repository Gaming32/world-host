package io.github.gaming32.worldhost.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public interface WorldHostPlatform {
    FriendlyByteBuf createByteBuf();

    void registerClientTickHandler(Consumer<Minecraft> handler);

    Services createServices();

    void showToast(ToastComponent toastComponent, SystemToast.SystemToastIds id, Component title, @Nullable Component message);

    void showProfileToast(UUID user, String title, Component description);

    ServerStatus parseServerStatus(FriendlyByteBuf buf);

    MutableComponent translatableComponent(String key);

    Component immutableComponent(String text);
}
