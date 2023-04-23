//#if FABRIC
package io.github.gaming32.worldhost.compat;

import io.github.gaming32.worldhost.gui.WorldHostConfigScreen;

//#if MC > 11601
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//#else
//$$ import io.github.prospector.modmenu.api.ConfigScreenFactory;
//$$ import io.github.prospector.modmenu.api.ModMenuApi;
//#endif

public class WorldHostModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WorldHostConfigScreen::new;
    }
}
//#endif
