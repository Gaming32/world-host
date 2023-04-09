package io.github.gaming32.worldhost;

import io.github.gaming32.worldhost._1_19_2.WorldHost_1_19_2;
import io.github.gaming32.worldhost._1_19_4.WorldHost_1_19_4;
import io.github.gaming32.worldhost.common.WorldHostCommon;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

public class MonojarEntrypoint implements ClientModInitializer {
    private static final VersionPredicate _1_19_2;
    private static final VersionPredicate _1_19_4;

    static {
        try {
            _1_19_2 = VersionPredicate.parse("1.19.2");
            _1_19_4 = VersionPredicate.parse(">=1.19.4");
        } catch (VersionParsingException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void onInitializeClient() {
        final Version minecraftVersion = FabricLoader.getInstance()
            .getModContainer("minecraft")
            .orElseThrow()
            .getMetadata()
            .getVersion();
        if (_1_19_2.test(minecraftVersion)) {
            init_1_19_2();
        } else if (_1_19_4.test(minecraftVersion)) {
            init_1_19_4();
        } else {
            throw new AssertionError();
        }
    }

    private static void init_1_19_2() {
        WorldHostCommon.init(new WorldHost_1_19_2());
    }

    private static void init_1_19_4() {
        WorldHostCommon.init(new WorldHost_1_19_4());
    }
}
