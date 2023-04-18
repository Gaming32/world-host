package io.github.gaming32.worldhost.compat;

//#if FABRIC
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
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
