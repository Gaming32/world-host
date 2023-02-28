package io.github.gaming32.worldhost.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.gaming32.worldhost.client.gui.WorldHostConfigScreen;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WorldHostConfigScreen::new;
    }
}
