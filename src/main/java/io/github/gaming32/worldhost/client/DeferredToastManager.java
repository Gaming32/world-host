package io.github.gaming32.worldhost.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeferredToastManager {
    private record ToastInfo(SystemToast.Type type, Text title, @Nullable Text description) {
    }

    private static List<ToastInfo> deferredToasts = new ArrayList<>();

    public static void show(SystemToast.Type type, Text title, @Nullable Text description) {
        final ToastInfo info = new ToastInfo(type, title, description);
        if (deferredToasts != null) {
            deferredToasts.add(info);
        } else {
            show(info);
        }
    }

    private static void show(ToastInfo info) {
        SystemToast.show(MinecraftClient.getInstance().getToastManager(), info.type, info.title, info.description);
    }

    public static void ready() {
        if (deferredToasts != null) {
            deferredToasts.forEach(DeferredToastManager::show);
            deferredToasts = null;
        }
    }
}
