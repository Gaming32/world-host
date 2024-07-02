package io.github.gaming32.worldhost.config;

import io.github.gaming32.worldhost.SecurityLevel;
import io.github.gaming32.worldhost.WorldHost;
import io.github.gaming32.worldhost.config.option.ConfigOption;
import io.github.gaming32.worldhost.config.option.ConfigOptions;
import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import io.github.gaming32.worldhost.testing.WorldHostTesting;
import org.quiltmc.parsers.json.JsonReader;
import org.quiltmc.parsers.json.JsonWriter;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class WorldHostConfig {
    public static final String DEFAULT_SERVER_IP = "world-host.jemnetworks.com";

    private String serverIp = DEFAULT_SERVER_IP;

    private OnlineStatusLocation onlineStatusLocation = OnlineStatusLocation.RIGHT;
    private boolean enableFriends = true;
    private boolean enableReconnectionToasts = false;
    private boolean noUPnP = false;
    private boolean useShortIp = false;
    private boolean showOutdatedWorldHost = true;
    private boolean shareButton = true;
    private boolean allowFriendRequests = true;
    private boolean announceFriendsOnline = true;
    private boolean whitelistJoins = false;
    private SecurityLevel requiredSecurityLevel = WorldHostTesting.ENABLED ? SecurityLevel.OFFLINE : SecurityLevel.SECURE;

    private final Set<UUID> friends = Collections.synchronizedSet(new LinkedHashSet<>());

    public void read(JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            final String key;
            switch (key = reader.nextName()) {
                case "serverIp" -> serverIp = reader.nextString();
                //noinspection DefaultNotLastCaseInSwitch
                default -> {
                    final ConfigOption<?> option = ConfigOptions.OPTIONS.get(key);
                    if (option != null) {
                        option.readValue(reader, this);
                    } else {
                        WorldHost.LOGGER.warn("Unknown WH config key {}. Skipping.", key);
                        reader.skipValue();
                    }
                }
                // Legacy updaters
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
                case "showOnlineStatus" -> {
                    WorldHost.LOGGER.info("Converting old showOnlineStatus to new onlineStatusLocation.");
                    onlineStatusLocation = reader.nextBoolean() ? OnlineStatusLocation.RIGHT : OnlineStatusLocation.OFF;
                }
                case "friends" -> {
                    WorldHost.LOGGER.info("Found old friends list.");
                    reader.beginArray();
                    friends.clear();
                    while (reader.hasNext()) {
                        friends.add(UUID.fromString(reader.nextString()));
                    }
                    reader.endArray();
                }
            }
        }
        reader.endObject();
    }

    public void readFriends(JsonReader reader) throws IOException {
        reader.beginArray();
        friends.clear();
        while (reader.hasNext()) {
            friends.add(UUID.fromString(reader.nextString()));
        }
        reader.endArray();
    }

    public void write(JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name("serverIp").value(serverIp);
        for (final var option : ConfigOptions.OPTIONS.values()) {
            writer.name(option.getName());
            option.writeValue(this, writer);
        }
        writer.endObject();
    }

    public void writeFriends(JsonWriter writer) throws IOException {
        writer.beginArray();
        for (final UUID friend : friends) {
            writer.value(friend.toString());
        }
        writer.endArray();
    }

    @ConfigProperty(order = 0)
    @ConfigProperty.StringDefault(DEFAULT_SERVER_IP)
    public String getServerIp() {
        return serverIp;
    }

    public void setServerIp(String serverIp) {
        this.serverIp = serverIp;
    }

    @ConfigProperty(order = 1)
    @ConfigProperty.EnumFallback("right")
    public OnlineStatusLocation getOnlineStatusLocation() {
        return onlineStatusLocation;
    }

    public void setOnlineStatusLocation(OnlineStatusLocation onlineStatusLocation) {
        this.onlineStatusLocation = onlineStatusLocation;
    }

    @ConfigProperty(order = 2)
    public boolean isEnableFriends() {
        return enableFriends;
    }

    public void setEnableFriends(boolean enableFriends) {
        this.enableFriends = enableFriends;
    }

    @ConfigProperty(order = 3)
    public boolean isEnableReconnectionToasts() {
        return enableReconnectionToasts;
    }

    public void setEnableReconnectionToasts(boolean enableReconnectionToasts) {
        this.enableReconnectionToasts = enableReconnectionToasts;
    }

    @ConfigProperty(order = 4)
    public boolean isNoUPnP() {
        return noUPnP;
    }

    public void setNoUPnP(boolean noUPnP) {
        this.noUPnP = noUPnP;
    }

    @ConfigProperty(order = 5)
    public boolean isUseShortIp() {
        return useShortIp;
    }

    public void setUseShortIp(boolean useShortIp) {
        this.useShortIp = useShortIp;
    }

    @ConfigProperty(order = 6)
    public boolean isShowOutdatedWorldHost() {
        return showOutdatedWorldHost;
    }

    public void setShowOutdatedWorldHost(boolean showOutdatedWorldHost) {
        this.showOutdatedWorldHost = showOutdatedWorldHost;
    }

    @ConfigProperty(order = 7)
    public boolean isShareButton() {
        return shareButton;
    }

    public void setShareButton(boolean shareButton) {
        this.shareButton = shareButton;
    }

    @ConfigProperty(order = 8)
    public boolean isAllowFriendRequests() {
        return allowFriendRequests;
    }

    public void setAllowFriendRequests(boolean allowFriendRequests) {
        this.allowFriendRequests = allowFriendRequests;
    }

    @ConfigProperty(order = 9)
    public boolean isAnnounceFriendsOnline() {
        return announceFriendsOnline;
    }

    public void setAnnounceFriendsOnline(boolean announceFriendsOnline) {
        this.announceFriendsOnline = announceFriendsOnline;
    }

    @ConfigProperty(order = 10)
    public boolean isWhitelistJoins() {
        return whitelistJoins;
    }

    public void setWhitelistJoins(boolean whitelistJoins) {
        this.whitelistJoins = whitelistJoins;
    }

    @ConfigProperty(order = 11)
    @ConfigProperty.EnumFallback("secure")
    public SecurityLevel getRequiredSecurityLevel() {
        return requiredSecurityLevel;
    }

    public void setRequiredSecurityLevel(SecurityLevel requiredSecurityLevel) {
        this.requiredSecurityLevel = requiredSecurityLevel;
    }

    public Set<UUID> getFriends() {
        return friends;
    }
}
