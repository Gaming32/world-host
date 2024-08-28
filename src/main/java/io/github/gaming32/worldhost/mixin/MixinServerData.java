package io.github.gaming32.worldhost.mixin;

import io.github.gaming32.worldhost.ext.ServerDataExt;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerData.class)
public class MixinServerData implements ServerDataExt {
    @Unique
    private Long wh$connectionId = null;

    @Override
    public Long wh$getConnectionId() {
        return wh$connectionId;
    }

    @Override
    public void wh$setConnectionId(Long connectionId) {
        wh$connectionId = connectionId;
    }
}
