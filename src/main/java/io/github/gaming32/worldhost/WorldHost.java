package io.github.gaming32.worldhost;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

//#if FABRIC
import net.fabricmc.api.ClientModInitializer;
//#endif

//#if FORGE
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.common.Mod;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//#endif

//#if FORGE
//$$ @Mod(WorldHost.MOD_ID)
//#endif
public class WorldHost
    //#if FABRIC
    implements ClientModInitializer
    //#endif
{
    public static final String MOD_ID = "world-host";

    public static final Logger LOGGER = LogUtils.getLogger();

    //#if FABRIC
    @Override
    public void onInitializeClient() {
        init();
    }
    //#endif

    private static void init() {
        LOGGER.info("World Host is initializing...");
    }

    //#if FORGE
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //$$ public static class ClientModEvents {
    //$$     @SubscribeEvent
    //$$     public static void onClientSetup(FMLClientSetupEvent event) {
    //$$         init();
    //$$     }
    //$$ }
    //#endif
}
