package io.github.gaming32.worldhost.plugin;

import io.github.gaming32.worldhost.WorldHost;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;

/**
 * World Host plugin entrypoint class. On Forge and NeoForge, annotate your implementation with {@link Entrypoint}. On
 * Fabric, reference this class with the <a href="https://fabricmc.net/wiki/documentation:entrypoint">entrypoint</a>
 * {@code worldhost}.
 */
public interface WorldHostPlugin {
    /**
     * Defines this plugin's priority. Plugins with a lower priority are run later.
     */
    default int priority() {
        return 0;
    }

    /**
     * Initializes this plugin. This is called after all plugins have been discovered and respects
     * {@link #priority plugin priority}.
     */
    default void init() {
    }

    /**
     * Gets information text to display on the specified {@link InfoTextsCategory category}.
     */
    default List<Component> getInfoTexts(InfoTextsCategory category) {
        return List.of();
    }

    /**
     * Pings the specified friends. The ping result should be placed in {@link WorldHost#ONLINE_FRIEND_PINGS}. Note
     * that this method receives all {@link OnlineFriend}s, not just ones loaded by this plugin.
     * @param friends The friends to ping
     */
    default void pingFriends(Collection<OnlineFriend> friends) {
    }

    /**
     * Refreshes the list of online friends. The results should be handled by calling
     * {@link WorldHost#friendWentOnline}.
     */
    default void refreshOnlineFriends() {
    }

    /**
     * Lists the friends to show in the friends list.
     * @param friendConsumer {@link Consumer} to invoke with every friend. This may be called from any thread.
     */
    default void listFriends(Consumer<FriendListFriend> friendConsumer) {
    }

    /**
     * Returns a {@link FriendAdder} if this plugin supports adding friends.
     */
    default Optional<FriendAdder> friendAdder() {
        return Optional.empty();
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
