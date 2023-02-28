package io.github.gaming32.worldhost;

import eu.midnightdust.lib.config.MidnightConfig;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class WorldHostData extends MidnightConfig {
    @Entry
    public static String serverIp = "world-host.jemnetworks.com:9646";

    @Entry
    @Hidden
    public static Set<UUID> friends = new LinkedHashSet<>();
}
