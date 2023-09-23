package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost.gui.OnlineStatusLocation;
import org.quiltmc.qup.json.JsonReader;
import org.quiltmc.qup.json.JsonWriter;

import java.io.IOException;
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
                case "onlineStatusLocation" -> {
                    final String value;
                    onlineStatusLocation = switch (value = reader.nextString()) {
                        case "left" -> OnlineStatusLocation.LEFT;
                        case "right" -> OnlineStatusLocation.RIGHT;
                        case "off" -> OnlineStatusLocation.OFF;
                        default -> {
                            WorldHost.LOGGER.warn("Unknown value for showOnlineStatus {}. Defaulting to right.", value);
                            yield OnlineStatusLocation.RIGHT;
                        }
                    };
                }
                case "showOnlineStatus" -> {
                    WorldHost.LOGGER.info("Converting old showOnlineStatus to new onlineStatusLocation.");
                    onlineStatusLocation = reader.nextBoolean() ? OnlineStatusLocation.RIGHT : OnlineStatusLocation.OFF;
                }
                case "enableFriends" -> enableFriends = reader.nextBoolean();
                case "enableReconnectionToasts" -> enableReconnectionToasts = reader.nextBoolean();
                case "noUPnP" -> noUPnP = reader.nextBoolean();
                case "useShortIp" -> useShortIp = reader.nextBoolean();
                case "showOutdatedWorldHost" -> showOutdatedWorldHost = reader.nextBoolean();
                case "shareButton" -> shareButton = reader.nextBoolean();
                case "allowFriendRequests" -> allowFriendRequests = reader.nextBoolean();
                case "announceFriendsOnline" -> announceFriendsOnline = reader.nextBoolean();
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
        writer.name("onlineStatusLocation").value(onlineStatusLocation.getSerializedName());
        writer.name("enableFriends").value(enableFriends);
        writer.name("enableReconnectionToasts").value(enableReconnectionToasts);
        writer.name("noUPnP").value(noUPnP);
        writer.name("useShortIp").value(useShortIp);
        writer.name("showOutdatedWorldHost").value(showOutdatedWorldHost);
        writer.name("shareButton").value(shareButton);
        writer.name("allowFriendRequests").value(allowFriendRequests);
        writer.name("announceFriendsOnline").value(announceFriendsOnline);

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

    public OnlineStatusLocation getOnlineStatusLocation() {
        return onlineStatusLocation;
    }

    public void setOnlineStatusLocation(OnlineStatusLocation onlineStatusLocation) {
        this.onlineStatusLocation = onlineStatusLocation;
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

    public boolean isNoUPnP() {
        return noUPnP;
    }

    public void setNoUPnP(boolean noUPnP) {
        this.noUPnP = noUPnP;
    }

    public boolean isUseShortIp() {
        return useShortIp;
    }

    public void setUseShortIp(boolean useShortIp) {
        this.useShortIp = useShortIp;
    }

    public boolean isShowOutdatedWorldHost() {
        return showOutdatedWorldHost;
    }

    public void setShowOutdatedWorldHost(boolean showOutdatedWorldHost) {
        this.showOutdatedWorldHost = showOutdatedWorldHost;
    }

    public boolean isShareButton() {
        return shareButton;
    }

    public void setShareButton(boolean shareButton) {
        this.shareButton = shareButton;
    }

    public boolean isAllowFriendRequests() {
        return allowFriendRequests;
    }

    public void setAllowFriendRequests(boolean allowFriendRequests) {
        this.allowFriendRequests = allowFriendRequests;
    }

    public boolean isAnnounceFriendsOnline() {
        return announceFriendsOnline;
    }

    public void setAnnounceFriendsOnline(boolean announceFriendsOnline) {
        this.announceFriendsOnline = announceFriendsOnline;
    }

    public Set<UUID> getFriends() {
        return friends;
    }
}
