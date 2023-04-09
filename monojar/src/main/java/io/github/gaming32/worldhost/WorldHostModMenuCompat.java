package io.github.gaming32.worldhost;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.gaming32.worldhost.common.WorldHostCommon;

public class WorldHostModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> WorldHostCommon.getPlatform().createConfigScreen(parent);
    }
}
