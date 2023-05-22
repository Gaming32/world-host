package io.github.gaming32.worldhost.gui;

import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

//#if MC >= 11904
import net.minecraft.client.gui.components.Tooltip;
//#endif

public abstract class CustomCycleButton<T, B extends CustomCycleButton<T, B>> extends Button {
    private final Consumer<B> onUpdate;

    private final T[] values;
    private int valueIndex;

    public CustomCycleButton(int x, int y, int width, int height, Consumer<B> onUpdate, T[] values) {
        this(x, y, width, height, null, onUpdate, values);
    }

    public CustomCycleButton(int x, int y, int width, int height, @Nullable Component tooltip, Consumer<B> onUpdate, T[] values) {
        super(
            x, y, width, height, Components.EMPTY, b -> {
                @SuppressWarnings("unchecked") final B cycle = (B)b;
                cycle.setValueIndex((cycle.getValueIndex() + 1) % cycle.getValues().length);
                cycle.getOnUpdate().accept(cycle);
            },
            //#if MC >= 11904
            DEFAULT_NARRATION
            //#else
            //$$ tooltip != null ? WorldHostScreen.onTooltip(tooltip) : NO_TOOLTIP
            //#endif
        );
        this.onUpdate = onUpdate;
        //#if MC >= 11904
        if (tooltip != null) {
            setTooltip(Tooltip.create(tooltip));
        }
        //#endif
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
    public abstract Component getMessage();
}
