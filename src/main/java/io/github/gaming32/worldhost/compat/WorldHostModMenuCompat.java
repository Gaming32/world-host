package io.github.gaming32.worldhost.compat;

//#if FABRIC
//#if MC > 11601
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
//#else
//$$ import io.github.prospector.modmenu.api.ConfigScreenFactory;
//$$ import io.github.prospector.modmenu.api.ModMenuApi;
//#endif
import io.github.gaming32.worldhost.gui.WorldHostConfigScreen;
//#endif

public class WorldHostModMenuCompat
    //#if FABRIC
    implements ModMenuApi
    //#endif
{
    //#if FABRIC
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WorldHostConfigScreen::new;
    }
    //#endif
}
