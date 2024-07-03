package io.github.gaming32.worldhost;

import org.quiltmc.parsers.json.JsonReader;
import org.semver4j.Semver;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class WorldHostUpdateChecker {
    public static final String MODRINTH_ID = "world-host";

    public static CompletableFuture<Optional<String>> checkForUpdates() {
        return WorldHost.httpGet(
            "https://api.modrinth.com/v2/project/" + MODRINTH_ID + "/version",
            builder -> builder
                .addParameter("game_versions", "[\"" + WorldHost.getModVersion("minecraft") + "\"]")
                .addParameter("loaders", "[\"" + WorldHost.MOD_LOADER + "\"]"),
            input -> {
                try (JsonReader reader = JsonReader.json(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    reader.beginArray();
                    if (!reader.hasNext()) {
                        return null;
                    }
                    reader.beginObject();
                    while (reader.hasNext()) {
                        final String key = reader.nextName();
                        if (!key.equals("version_number")) {
                            reader.skipValue();
                            continue;
                        }
                        return reader.nextString();
                    }
                    return null;
                }
            }
        ).<Optional<String>>thenApply(latestVersion -> {
            if (latestVersion == null) {
                return Optional.empty();
            }
            if (new Semver(WorldHost.getModVersion(WorldHost.MOD_ID)).compareTo(new Semver(latestVersion)) >= 0) {
                return Optional.empty();
            }
            return Optional.of(latestVersion);
        }).exceptionally(t -> {
            WorldHost.LOGGER.error("Failed to check for updates", t);
            return Optional.empty();
        });
    }

    public static String formatUpdateLink(String version) {
        return "https://modrinth.com/mod/" + MODRINTH_ID + "/version/" + version;
    }
}
