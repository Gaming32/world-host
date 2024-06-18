//#if FABRIC
package io.github.gaming32.worldhost.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.gaming32.worldhost.gui.screen.WorldHostConfigScreen;

//#if MC >= 1.20.6
import com.terraformersmc.modmenu.api.UpdateChannel;
import com.terraformersmc.modmenu.api.UpdateChecker;
import com.terraformersmc.modmenu.api.UpdateInfo;
import io.github.gaming32.worldhost.WorldHostUpdateChecker;
//#endif

public class WorldHostModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WorldHostConfigScreen::new;
    }

    //#if MC >= 1.20.6
    @Override
    public UpdateChecker getUpdateChecker() {
        return () -> WorldHostUpdateChecker.checkForUpdates()
            .join()
            .map(version -> new UpdateInfo() {
                @Override
                public boolean isUpdateAvailable() {
                    return true;
                }

                @Override
                public String getDownloadLink() {
                    return WorldHostUpdateChecker.formatUpdateLink(version);
                }

                @Override
                public UpdateChannel getUpdateChannel() {
                    return UpdateChannel.RELEASE;
                }
            })
            .orElse(null);
    }
    //#endif
}
//#endif
