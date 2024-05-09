package io.github.gaming32.worldhost.gui.widget;

import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

//#if MC >= 1.19.4
import net.minecraft.client.gui.components.Tooltip;
//#else
//$$ import io.github.gaming32.worldhost.gui.screen.WorldHostScreen;
//#endif

public abstract class CustomCycleButton<T, B extends CustomCycleButton<T, B>> extends Button {
    private final Component title;
    @Nullable
    private final Map<Component, Component> messages;

    private final Consumer<B> onUpdate;

    private final T[] values;
    private int valueIndex;

    public CustomCycleButton(
        int x, int y,
        int width, int height,
        @Nullable Component title,
        Consumer<B> onUpdate, T[] values
    ) {
        this(x, y, width, height, title, null, onUpdate, values);
    }

    public CustomCycleButton(
        int x, int y,
        int width, int height,
        @Nullable Component title, @Nullable Component tooltip,
        Consumer<B> onUpdate, T[] values
    ) {
        super(
            x, y, width, height, Components.EMPTY, b -> {
                @SuppressWarnings("unchecked") final B cycle = (B)b;
                final int add = Screen.hasShiftDown() ? -1 : 1;
                cycle.setValueIndex(Math.floorMod(cycle.getValueIndex() + add, cycle.getValues().length));
                cycle.getOnUpdate().accept(cycle);
            },
            //#if MC >= 1.19.4
            DEFAULT_NARRATION
            //#else
            //$$ tooltip != null ? WorldHostScreen.onTooltip(tooltip) : NO_TOOLTIP
            //#endif
        );

        this.title = title;
        messages = title != null ? new WeakHashMap<>() : null;

        //#if MC >= 1.19.4
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
        //#endif

        this.onUpdate = onUpdate;
        this.values = values;
    }

    public T getValue() {
        return values[valueIndex];
    }

    public Consumer<B> getOnUpdate() {
        return onUpdate;
    }

    protected T[] getValues() {
        return values;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public void setValueIndex(int index) {
        valueIndex = index;
    }

    @Override
    public final void setMessage(@NotNull Component message) {
        throw new UnsupportedOperationException("Cannot set message of " + getClass().getSimpleName());
    }

    @NotNull
    @Override
    public Component getMessage() {
        final Component valueMessage = getValueMessage();
        if (messages == null) {
            return valueMessage;
        }
        Component result = messages.get(valueMessage);
        if (result != null) {
            return result;
        }
        result = title.copy().append(": ").append(valueMessage);
        messages.put(valueMessage, result);
        return result;
    }

    //#if MC >= 1.19.4
    @Override
    public final void setTooltip(@Nullable Tooltip tooltip) {
        super.setTooltip(tooltip);
    }
    //#endif

    @NotNull
    public abstract Component getValueMessage();
}
