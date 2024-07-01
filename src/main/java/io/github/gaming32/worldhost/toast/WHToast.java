package io.github.gaming32.worldhost.toast;

import com.demonwav.mcdev.annotations.Translatable;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.gaming32.worldhost.ResourceLocations;
import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
import io.github.gaming32.worldhost.testing.WorldHostTesting;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#endif

public class WHToast {
    private static final int GAP = 4;
    private static final int X_OFFSET = 4;
    private static final int Y_OFFSET = 4;

    private static final SoundEvent IMPORTANT = newSoundEvent("important");
    private static final SoundEvent REGULAR = newSoundEvent("regular");
    private static final SoundEvent REMOVED = newSoundEvent("removed");

    private static boolean ready = false;
    private static final List<ToastInstance> TOASTS = new ArrayList<>();

    public static ToastBuilder builder(@NotNull Component title) {
        return new ToastBuilder(title);
    }

    public static ToastBuilder builder(@NotNull @Translatable String titleKey) {
        return new ToastBuilder(Components.translatable(titleKey));
    }

    public static void ready() {
        float y = Y_OFFSET;
        for (final ToastInstance toast : TOASTS) {
            toast.calculateText();
            toast.y = y;
            y += toast.height + GAP;
        }
        if (!TOASTS.isEmpty()) {
            if (TOASTS.stream().anyMatch(t -> t.important)) {
                playSound(IMPORTANT);
            } else {
                playSound(REGULAR);
            }
        }
        ready = true;
    }

    static void add(ToastInstance toast) {
        if (ready) {
            toast.calculateText();
            playSound(toast.important ? IMPORTANT : REGULAR);
            if (!TOASTS.isEmpty()) {
                if (Y_OFFSET + 2 * toast.height + GAP <= TOASTS.getFirst().y) {
                    toast.y = Y_OFFSET;
                    TOASTS.addFirst(toast);
                    return;
                }
                for (int i = 0; i < TOASTS.size() - 1; i++) {
                    final ToastInstance prevToast = TOASTS.get(i);
                    final ToastInstance nextToast = TOASTS.get(i + 1);
                    if (prevToast.y + prevToast.height + GAP + 2 * toast.height + GAP <= nextToast.y) {
                        toast.y = prevToast.y + prevToast.height + GAP;
                        TOASTS.add(i + 1, toast);
                        return;
                    }
                }
                final ToastInstance lastToast = TOASTS.getLast();
                toast.y = lastToast.y + lastToast.height + GAP;
            } else {
                toast.y = Y_OFFSET;
            }
        }
        TOASTS.add(toast);
    }

    public static void tick() {
        if (!ready) return;

        final var it = TOASTS.iterator();
        while (it.hasNext()) {
            final ToastInstance toast = it.next();
            toast.ticksRemaining--;
            if (toast.ticksRemaining == 19) {
                playSound(REMOVED);
            } else if (toast.ticksRemaining <= 0) {
                it.remove();
            }
        }
    }

    public static void render(
        @NotNull
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float tickDelta
    ) {
        if (!ready) return;

        final PoseStack poseStack = WorldHostScreen.pose(context);
        poseStack.pushPose();
        poseStack.translate(0f, 0f, 100f);

        final var window = Minecraft.getInstance().getWindow();
        final int screenWidth = window.getGuiScaledWidth();
        final int screenHeight = window.getGuiScaledHeight();

        for (final ToastInstance toast : TOASTS) {
            toast.render(
                context,
                screenWidth - X_OFFSET - toast.width,
                screenHeight - toast.y - toast.height,
                mouseX, mouseY, tickDelta
            );
        }

        poseStack.popPose();
    }

    public static boolean click(double mouseX, double mouseY, int button) {
        if (!ready || WorldHostTesting.ENABLED) {
            return false;
        }

        final var window = Minecraft.getInstance().getWindow();
        final int screenWidth = window.getGuiScaledWidth();
        final int screenHeight = window.getGuiScaledHeight();

        for (final ToastInstance toast : TOASTS) {
            final float toastX = screenWidth - X_OFFSET - toast.width;
            final float toastY = screenHeight - toast.height - toast.y;
            if (
                mouseX >= toastX && mouseX <= toastX + toast.width &&
                    mouseY >= toastY && mouseY <= toastY + toast.height &&
                    toast.click(button)
            ) {
                return true;
            }
        }

        return false;
    }

    private static SoundEvent newSoundEvent(String location) {
        //#if MC >= 1.19.4
        return SoundEvent.createVariableRangeEvent(
        //#else
        //$$ return new SoundEvent(
        //#endif
            ResourceLocations.worldHost("toast/" + location)
        );
    }

    private static void playSound(SoundEvent event) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(event, 1f, 1f));
    }
}
