package io.github.gaming32.worldhost.plugin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Represents the joinability of a friend.
 */
public sealed interface Joinability extends Comparable<Joinability> {
    /**
     * The reason, if specified.
     */
    Optional<Component> reason();

    /**
     * The formatting for this joinability. Typically passed into
     * {@link MutableComponent#withStyle(UnaryOperator) MutableComponent.withStyle}.
     */
    UnaryOperator<Style> nameFormatting();

    /**
     * Whether this joinability is joinable.
     */
    boolean canJoin();

    /**
     * The ordinal of this type of joinability. Mirrors {@link Enum#ordinal() Enum.ordinal}.
     */
    int ordinal();

    /**
     * Compares this joinability's type to {@code o}. Mirrors {@link Enum#compareTo}. The {@link #reason} is ignored.
     */
    @Override
    default int compareTo(@NotNull Joinability o) {
        return ordinal() - o.ordinal();
    }

    /**
     * Represents an unjoinable friend.
     * @param reason The reason why the friend cannot be joined
     */
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

    /**
     * Represents a friend that can be joined, but potentially unstably.
     * @param reason The reason why joining this friend may be unstable.
     */
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

    /**
     * Represents a friend that can be joined without any major issue.
     */
    final class Joinable implements Joinability {
        /**
         * The sole instance of {@link Joinable}.
         */
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
