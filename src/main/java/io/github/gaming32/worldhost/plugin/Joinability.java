package io.github.gaming32.worldhost.plugin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.UnaryOperator;

public sealed interface Joinability extends Comparable<Joinability> {
    Optional<Component> reason();

    UnaryOperator<Style> nameFormatting();

    boolean canJoin();

    int ordinal();

    @Override
    default int compareTo(@NotNull Joinability o) {
        return ordinal() - o.ordinal();
    }

    record Unjoinable(Optional<Component> reason) implements Joinability {
        public Unjoinable(Component reason) {
            this(Optional.of(reason));
        }

        public Unjoinable() {
            this(Optional.empty());
        }

        @Override
        public int ordinal() {
            return 0;
        }

        @Override
        public UnaryOperator<Style> nameFormatting() {
            return style -> style.withColor(ChatFormatting.RED);
        }

        @Override
        public boolean canJoin() {
            return false;
        }
    }

    record JoinableWithWarning(Optional<Component> reason) implements Joinability {
        public JoinableWithWarning(Component reason) {
            this(Optional.of(reason));
        }

        public JoinableWithWarning() {
            this(Optional.empty());
        }

        @Override
        public int ordinal() {
            return 1;
        }

        @Override
        public UnaryOperator<Style> nameFormatting() {
            return style -> style.withColor(ChatFormatting.GOLD);
        }

        @Override
        public boolean canJoin() {
            return true;
        }
    }

    final class Joinable implements Joinability {
        public static final Joinable INSTANCE = new Joinable();

        private Joinable() {
        }

        @Override
        public String toString() {
            return "Joinable";
        }

        @Override
        public int ordinal() {
            return 2;
        }

        @Override
        public Optional<Component> reason() {
            return Optional.empty();
        }

        @Override
        public UnaryOperator<Style> nameFormatting() {
            return UnaryOperator.identity();
        }

        @Override
        public boolean canJoin() {
            return true;
        }
    }
}
