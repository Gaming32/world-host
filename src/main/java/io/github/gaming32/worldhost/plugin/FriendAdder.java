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

    boolean delayLookup(String name);
}
