package io.github.gaming32.worldhost.testing;

import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayDeque;
import java.util.Deque;

public class ScreenChain {
    private final Deque<ChainElement> chain = new ArrayDeque<>();

    private ScreenChain() {
    }

    public static ScreenChain start() {
        return new ScreenChain();
    }

    public ScreenChain then(Class<? extends Screen> screenClass, Runnable onReady) {
        chain.add(new IsChainElement(screenClass, onReady));
        return this;
    }

    public ScreenChain maybe(Class<? extends Screen> screenClass, Runnable onReady) {
        chain.add(new IfChainElement(screenClass, onReady));
        return this;
    }

    @SafeVarargs
    public final ScreenChain skip(Class<? extends Screen>... screenClasses) {
        for (final var screenClass : screenClasses) {
            then(screenClass, () -> {});
        }
        return this;
    }

    public ScreenChain skipAbsentScreen() {
        return then(null, () -> {});
    }

    public ScreenChain then(ScreenChain other) {
        chain.addAll(other.chain);
        return this;
    }

    public void advance(Screen screen) {
        final ChainElement next = chain.poll();
        if (next == null) {
            throw new IllegalStateException("ScreenChain ended before test end. Remaining screen: " + screen);
        }
        switch (next.advance(screen)) {
            case SUCCESS -> {}
            case ADVANCE_FURTHER -> advance(screen);
            case ERROR -> throw new IllegalStateException("ChainElement " + next + " failed on " + screen);
        }
    }

    public static Class<? extends Screen> getScreenClass(Screen screen) {
        return screen != null ? screen.getClass() : null;
    }

    private interface ChainElement {
        AdvanceMode advance(Screen screen);

        String toString();
    }

    private enum AdvanceMode {
        SUCCESS, ADVANCE_FURTHER, ERROR
    }

    private record IsChainElement(Class<? extends Screen> screenClass, Runnable onScreenReady) implements ChainElement {
        @Override
        public AdvanceMode advance(Screen screen) {
            if (getScreenClass(screen) == screenClass) {
                onScreenReady.run();
                return AdvanceMode.SUCCESS;
            }
            return AdvanceMode.ERROR;
        }

        @Override
        public String toString() {
            return "Is[" + screenClass + "]";
        }
    }

    private record IfChainElement(Class<? extends Screen> screenClass, Runnable onScreenReady) implements ChainElement {
        @Override
        public AdvanceMode advance(Screen screen) {
            if (getScreenClass(screen) == screenClass) {
                onScreenReady.run();
                return AdvanceMode.SUCCESS;
            }
            return AdvanceMode.ADVANCE_FURTHER;
        }

        @Override
        public String toString() {
            return "If[" + screenClass + "]";
        }
    }
}
