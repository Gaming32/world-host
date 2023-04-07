package io.github.gaming32.worldhost._1_19_2;

import io.github.gaming32.worldhost._1_19_2.mixin.client.MinecraftAccessor;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import io.github.gaming32.worldhost.common.WorldHostPlatform;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.Services;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class WorldHost1_19_2 implements WorldHostPlatform, ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldHostCommon.init(this);
    }

    @Override
    public FriendlyByteBuf createByteBuf() {
        return PacketByteBufs.create();
    }

    @Override
    public void registerClientTickHandler(Consumer<Minecraft> handler) {
        ClientTickEvents.END_CLIENT_TICK.register(handler::accept);
    }

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
    public MutableComponent translatableComponent(String key) {
        return Component.translatable(key);
    }

    @Override
    public Component immutableComponent(String text) {
        return Component.nullToEmpty(text);
    }
}
