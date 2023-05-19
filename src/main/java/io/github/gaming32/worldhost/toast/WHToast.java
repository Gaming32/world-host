package io.github.gaming32.worldhost.toast;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

public class WHToast {
    private static final int GAP = 4;
    private static final int X_OFFSET = 4;
    private static final int Y_OFFSET = 4;

    private static boolean ready = false;
    static final Deque<ToastInstance> TOASTS = new ArrayDeque<>();

    public static ToastBuilder builder(@NotNull Component title) {
        return new ToastBuilder(title);
    }

    public static ToastBuilder builder(@NotNull String titleKey) {
        return new ToastBuilder(Components.translatable(titleKey));
    }

    public static void ready() {
        ready = true;
    }

    public static void tick() {
        if (!ready) return;

        final var it = TOASTS.iterator();
        float shift = 0;
        while (it.hasNext()) {
            final ToastInstance toast = it.next();
            toast.yShift += shift;
            shift = 0;
            if (--toast.ticksRemaining <= 0) {
                it.remove();
                shift = toast.height + GAP + toast.yShift;
            }
        }

        for (final ToastInstance toast : TOASTS) {
            toast.prevYShift = toast.yShift;
            toast.yShift = Mth.lerp(0.25f, toast.yShift, 0);
            if (toast.yShift < 0.1f) {
                toast.yShift = 0f;
            }
        }
    }

    public static void render(PoseStack poseStack, int mouseX, int mouseY, float tickDelta) {
        if (!ready) return;

        poseStack.pushPose();
        poseStack.translate(0f, 0f, 100f);

        final Window window = Minecraft.getInstance().getWindow();
        final int screenWidth = window.getGuiScaledWidth();
        final int screenHeight = window.getGuiScaledHeight();

        float y = Y_OFFSET;

        for (final ToastInstance toast : TOASTS) {
            toast.render(
                poseStack,
                screenWidth - X_OFFSET - toast.width,
                screenHeight - y - Mth.lerp(tickDelta, toast.prevYShift, toast.yShift) - toast.height,
                mouseX, mouseY, tickDelta
            );
            y += toast.height + GAP + toast.yShift;
        }

        poseStack.popPose();
    }

    public static boolean click(double mouseX, double mouseY) {
        if (!ready) {
            return false;
        }

        final Window window = Minecraft.getInstance().getWindow();
        final int screenWidth = window.getGuiScaledWidth();
        final int screenHeight = window.getGuiScaledHeight();

        float y = Y_OFFSET;
        for (final ToastInstance toast : TOASTS) {
            final float toastX = screenWidth - X_OFFSET - toast.width;
            final float toastY = screenHeight - y - toast.height - toast.yShift;
            if (mouseX >= toastX && mouseX <= toastX + toast.width && mouseY >= toastY && mouseY <= toastY + toast.height && toast.click()) {
                return true;
            }
            y += toast.height + GAP + toast.yShift;
        }

        return false;
    }
}
