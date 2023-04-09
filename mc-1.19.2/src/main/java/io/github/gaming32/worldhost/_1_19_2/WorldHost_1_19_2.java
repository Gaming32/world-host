package io.github.gaming32.worldhost._1_19_2;

import io.github.gaming32.worldhost._1_19_2.gui.WorldHostConfigScreen;
import io.github.gaming32.worldhost._1_19_2.mixin.client.MinecraftAccessor;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

public class WorldHost_1_19_2 implements WorldHostPlatform {
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
    public Screen createConfigScreen(Screen parent) {
        return new WorldHostConfigScreen(parent);
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
}
