package io.github.gaming32.worldhost.plugin;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public interface FriendAdder {
    Component label();

    /**
     * Search for users by name.
     * @param name The search query
     * @param maxResults A hint at the maximum result count. May more be returned, but might not be shown.
     * @param friendConsumer The {@link Consumer} to pass found users. This may be called from any thread.
     */
    void searchFriends(String name, int maxResults, Consumer<FriendListFriend> friendConsumer);

    /**
     * Determine whether searching name is expensive to look up and should be delayed until the user has quit typing.
     * @param name The name to check for delay requirements.
     * @return Whether to delay the search until the user has quit typing.
     */
    boolean delayLookup(String name);

    /**
     * The maximum length for a valid username. Names longer than this will not be given to this {@link FriendAdder}.
     * @return The maximum valid username length;
     */
    int maxValidNameLength();
}
