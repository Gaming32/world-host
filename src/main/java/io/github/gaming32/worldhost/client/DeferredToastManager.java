package io.github.gaming32.worldhost.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeferredToastManager {
    @Nullable
    public static IconRenderer queuedCustomIcon;

    @FunctionalInterface
    public interface IconRenderer {
        void draw(PoseStack matrices, int x, int y);
    }

    private record ToastInfo(SystemToast.SystemToastIds type, IconRenderer icon, Component title, @Nullable Component description) {
    }

    private static List<ToastInfo> deferredToasts = new ArrayList<>();

    public static void show(SystemToast.SystemToastIds type, Component title, @Nullable Component description) {
        show(type, null, title, description);
    }

    public static void show(SystemToast.SystemToastIds type, IconRenderer icon, Component title, @Nullable Component description) {
        final ToastInfo toast = new ToastInfo(type, icon, title, description);
        if (deferredToasts != null) {
            deferredToasts.add(toast);
        } else {
            show(toast);
        }
    }

    private static void show(ToastInfo toast) {
        Minecraft.getInstance().execute(() -> {
            queuedCustomIcon = toast.icon;
            SystemToast.addOrUpdate(Minecraft.getInstance().getToasts(), toast.type, toast.title, Objects.requireNonNullElse(toast.description, CommonComponents.EMPTY));
        });
    }

    public static void ready() {
        if (deferredToasts != null) {
            deferredToasts.forEach(DeferredToastManager::show);
            deferredToasts = null;
        }
    }
}
