package io.github.gaming32.worldhost;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class WorldHostConfig {
    private String serverIp = "world-host.jemnetworks.com";

    private boolean showOnlineStatus = true;

    private boolean enableFriends = true;

    private boolean enableReconnectionToasts = false;

    private final Set<UUID> friends = new LinkedHashSet<>();

    public void read(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key;
            switch (key = reader.nextName()) {
                case "serverIp" -> serverIp = reader.nextString();
                case "serverUri" -> {
                    WorldHost.LOGGER.info("Found old-style serverUri. Converting to new-style serverIp.");
                    final String serverUri = reader.nextString();
                    final int index = serverUri.indexOf("://");
                    if (index == -1) {
                        WorldHost.LOGGER.warn("Invalid serverUri. Missing ://");
                        serverIp = serverUri;
                        continue;
                    }
                    serverIp = serverUri.substring(index + 3);
                    if (serverIp.endsWith(":9646")) {
                        serverIp = serverIp.substring(0, serverIp.length() - 5);
                    }
                }
                case "showOnlineStatus" -> showOnlineStatus = reader.nextBoolean();
                case "enableFriends" -> enableFriends = reader.nextBoolean();
                case "enableReconnectionToasts" -> enableReconnectionToasts = reader.nextBoolean();
                case "friends" -> {
                    friends.clear();
                    reader.beginArray();
                    while (reader.hasNext()) {
                        friends.add(UUID.fromString(reader.nextString()));
                    }
                    reader.endArray();
                }
                default -> {
                    WorldHost.LOGGER.warn("Unknown WH config key {}. Skipping.", key);
                    reader.skipValue();
                }
            }
        }
        reader.endObject();
    }

    public void write(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("serverIp").value(serverIp);
        writer.name("showOnlineStatus").value(showOnlineStatus);
        writer.name("enableFriends").value(enableFriends);
        writer.name("enableReconnectionToasts").value(enableReconnectionToasts);

        writer.name("friends").beginArray();
        for (final UUID friend : friends) {
            writer.value(friend.toString());
        }
        writer.endArray();

        writer.endObject();
    }

    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    public boolean isShowOnlineStatus() {
        return showOnlineStatus;
    }

    public void setShowOnlineStatus(boolean showOnlineStatus) {
        this.showOnlineStatus = showOnlineStatus;
    }

    public boolean isEnableFriends() {
        return enableFriends;
    }

    public void setEnableFriends(boolean enableFriends) {
        this.enableFriends = enableFriends;
    }

    public boolean isEnableReconnectionToasts() {
        return enableReconnectionToasts;
    }

    public void setEnableReconnectionToasts(boolean enableReconnectionToasts) {
        this.enableReconnectionToasts = enableReconnectionToasts;
    }

    public Set<UUID> getFriends() {
        return friends;
    }
}
