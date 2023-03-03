package io.github.gaming32.worldhost.client;

import io.github.gaming32.worldhost.ext.TutorialToastExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DeferredToastManager {
    private sealed interface ToastInfo {
    }

    private record SystemToastInfo(SystemToast.Type type, Text title, @Nullable Text description) implements ToastInfo {
    }

    private record IconToastInfo(TutorialToastExt.IconRenderer icon, Text title, @Nullable Text description) implements ToastInfo {
    }

    private static List<ToastInfo> deferredToasts = new ArrayList<>();

    public static void show(SystemToast.Type type, Text title, @Nullable Text description) {
        showOrDefer(new SystemToastInfo(type, title, description != null ? description : Text.empty()));
    }

    public static void show(TutorialToastExt.IconRenderer icon, Text title, @Nullable Text description) {
        showOrDefer(new IconToastInfo(icon, title, description));
    }

    private static void showOrDefer(ToastInfo info) {
        if (deferredToasts != null) {
            deferredToasts.add(info);
        } else {
            show(info);
        }
    }

    private static void show(ToastInfo info) {
        final ToastManager toastManager = MinecraftClient.getInstance().getToastManager();
        if (info instanceof SystemToastInfo systemToast) {
            SystemToast.show(toastManager, systemToast.type, systemToast.title, systemToast.description);
        } else if (info instanceof IconToastInfo iconToast) {
            final TutorialToast toast = new TutorialToast(null, iconToast.title, iconToast.description, false);
            ((TutorialToastExt)toast).setCustomIcon(iconToast.icon);
            toastManager.add(toast);
        } else {
            throw new AssertionError("https://github.com/Gaming32/world-host/issues");
        }
    }

    public static void ready() {
        if (deferredToasts != null) {
            deferredToasts.forEach(DeferredToastManager::show);
            deferredToasts = null;
        }
    }
}
