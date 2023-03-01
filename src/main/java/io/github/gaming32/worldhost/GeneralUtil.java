package io.github.gaming32.worldhost;

import com.mojang.authlib.GameProfile;
import org.apache.commons.lang3.StringUtils;

public class GeneralUtil {
    public static String getName(GameProfile profile) {
        return StringUtils.getIfBlank(profile.getName(), () -> profile.getId().toString());
    }
}
