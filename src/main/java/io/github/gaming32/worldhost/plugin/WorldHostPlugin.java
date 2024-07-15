package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * World Host plugin entrypoint class. On Forge and NeoForge, annotate your implementation with {@link Entrypoint}. On
 * Fabric, reference this class with the <a href="https://fabricmc.net/wiki/documentation:entrypoint">entrypoint</a>
 * {@code worldhost}.
 */
public interface WorldHostPlugin {
    default int priority() {
        return 0;
    }

    default void init() {
    }

    default List<Component> getInfoTexts(InfoTextsCategory category) {
        return List.of();
    }

    default void pingFriends(Collection<OnlineFriend> friends) {
    }

    default void refreshOnlineFriends() {
    }

    default void listFriends(Consumer<FriendListFriend> friendConsumer) {
    }

    /**
     * World Host plugin entrypoint class marker. This is used on Forge and NeoForge. On Fabric, use the
     * <a href="https://fabricmc.net/wiki/documentation:entrypoint">entrypoint system</a>.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Entrypoint {
    }
}
