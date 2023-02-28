package io.github.gaming32.worldhost;

import com.mojang.logging.LogUtils;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.File;

public class WorldHost implements ModInitializer {
    public static final String MOD_ID = "world-host";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final File CACHE_DIR = FabricLoader.getInstance()
        .getGameDir()
        .resolve(".world-host-cache")
        .toFile();

    @Override
    public void onInitialize() {
        MidnightConfig.init(MOD_ID, WorldHostData.class);
    }
}
