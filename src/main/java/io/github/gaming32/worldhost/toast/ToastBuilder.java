package io.github.gaming32.worldhost.toast;

import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ToastBuilder {
    @NotNull
    private final Component title;
    @Nullable
    private Component description = null;
    @Nullable
    private IconRenderer iconRenderer = null;
    @Nullable
    private Runnable clickAction = null;
    private int ticks = 100;

    ToastBuilder(@NotNull Component title) {
        this.title = title;
    }

    public ToastBuilder description(@Nullable Component description) {
        this.description = description;
        return this;
    }

    public ToastBuilder description(@Nullable String translationKey) {
        return description(Components.translatable(translationKey));
    }

    public ToastBuilder icon(IconRenderer iconRenderer) {
        this.iconRenderer = iconRenderer;
        return this;
    }

    public ToastBuilder clickAction(Runnable clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    public ToastBuilder ticks(int ticks) {
        this.ticks = ticks;
        return this;
    }

    public void show() {
        Minecraft.getInstance().execute(() -> WHToast.TOASTS.add(new ToastInstance(title, description, iconRenderer, clickAction, ticks)));
    }
}
