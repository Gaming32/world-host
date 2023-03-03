package io.github.gaming32.worldhost.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeferredToastManager {
    @Nullable
    public static IconRenderer queuedCustomIcon;

    @FunctionalInterface
    public interface IconRenderer {
        void draw(MatrixStack matrices, int x, int y);
    }

    private record ToastInfo(SystemToast.Type type, IconRenderer icon, Text title, @Nullable Text description) {
    }

    private static List<ToastInfo> deferredToasts = new ArrayList<>();

    public static void show(SystemToast.Type type, Text title, @Nullable Text description) {
        show(type, null, title, description);
    }

    public static void show(SystemToast.Type type, IconRenderer icon, Text title, @Nullable Text description) {
        final ToastInfo toast = new ToastInfo(type, icon, title, description);
        if (deferredToasts != null) {
            deferredToasts.add(toast);
        } else {
            show(toast);
        }
    }

    private static void show(ToastInfo toast) {
        queuedCustomIcon = toast.icon;
        SystemToast.show(MinecraftClient.getInstance().getToastManager(), toast.type, toast.title, toast.description);
    }

    public static void ready() {
        if (deferredToasts != null) {
            deferredToasts.forEach(DeferredToastManager::show);
            deferredToasts = null;
        }
    }
}
