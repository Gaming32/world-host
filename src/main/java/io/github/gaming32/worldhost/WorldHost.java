package io.github.gaming32.worldhost;

//#if MC >= 11700
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
//#else
//$$ import org.apache.logging.log4j.LogManager;
//$$ import org.apache.logging.log4j.Logger;
//#endif

//#if FABRIC
import net.fabricmc.api.ClientModInitializer;
//#endif

//#if FORGE
//$$ import net.minecraftforge.fml.common.Mod;
//#if MC > 11202
//$$ import net.minecraftforge.api.distmarker.Dist;
//$$ import net.minecraftforge.eventbus.api.SubscribeEvent;
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//#else
//$$ import net.minecraftforge.fml.common.event.FMLInitializationEvent;
//#endif
//#endif

//#if FORGE
//#if MC > 11202
//$$ @Mod(WorldHost.MOD_ID)
//#else
//$$ @Mod(modid = WorldHost.MOD_ID, useMetadata = true, clientSideOnly = true)
//#endif
//#endif
public class WorldHost
    //#if FABRIC
    implements ClientModInitializer
    //#endif
{
    public static final String MOD_ID =
        //#if FORGE
        //$$ "world_host";
        //#else
        "world-host";
        //#endif

    public static final Logger LOGGER =
        //#if MC >= 11700
        LogUtils.getLogger();
        //#else
        //$$ LogManager.getLogger();
        //#endif

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
    //#if MC > 11202
    //$$ @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    //$$ public static class ClientModEvents {
    //$$     @SubscribeEvent
    //$$     public static void onClientSetup(FMLClientSetupEvent event) {
    //$$         init();
    //$$     }
    //$$ }
    //#else
    //$$ @Mod.EventHandler
    //$$ public void init(FMLInitializationEvent event) {
    //$$     init();
    //$$ }
    //#endif
    //#endif
}
