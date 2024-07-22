package io.github.gaming32.worldhost;

import com.google.common.base.Suppliers;
import com.mojang.authlib.GameProfile;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//#endif

//#if MC >= 1.19.2
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.server.WorldLoader;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
//#else
//$$ import net.minecraft.world.level.dimension.DimensionType;
//#endif

//#if MC >= 1.19.4
import net.minecraft.client.telemetry.TelemetryEventSender;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
//#else
//$$ import com.mojang.datafixers.util.Pair;
//$$ import com.mojang.serialization.Lifecycle;
//$$ import net.minecraft.core.Registry;
//$$ import net.minecraft.core.RegistryAccess;
//$$ import net.minecraft.server.packs.PackType;
//$$ import net.minecraft.world.level.DataPackConfig;
//#endif

//#if MC >= 1.20.4
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.world.flag.FeatureFlags;
//#endif

//#if MC >= 1.20.6
import java.util.Map;
//#endif

//#if MC >= 1.21
import java.util.List;
import net.minecraft.server.ServerLinks;
//#endif

//#if NEOFORGE
//$$ import net.neoforged.neoforge.network.connection.ConnectionType;
//#endif

public final class GameProfileRenderer {
    private static final Supplier<ClientLevel> FAKE_LEVEL = Suppliers.memoize(GameProfileRenderer::createFakeLevel);

    private final Player player;

    private GameProfileRenderer(RemotePlayer player) {
        this.player = player;
    }

    public static GameProfileRenderer create(GameProfile profile) {
        final RemotePlayer player = new RemotePlayer(
            FAKE_LEVEL.get(), profile
            //#if MC == 1.19.2
            //$$ , null
            //#endif
        );
        player.setPos(0, 80, 0);
        return new GameProfileRenderer(player);
    }

    public void renderFacingMouse(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int x1, int y1, int x2, int y2, int scale, float mouseX, float mouseY
    ) {
        final EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        final Camera oldCamera = entityRenderDispatcher.camera;
        entityRenderDispatcher.camera = new Camera();
        //#if MC >= 1.19.4
        context.pose().pushPose();
        context.pose().translate(0f, 0f, 1000f);
        InventoryScreen.renderEntityInInventoryFollowsMouse(
            context, x1, y1, x2, y2, scale, 0.0625f, mouseX, mouseY, player
        );
        context.pose().popPose();
        //#else
        //$$ final int centerX = (x1 + x2) / 2;
        //$$ final int centerY = (y1 + y2) / 2;
        //$$ InventoryScreen.renderEntityInInventory(
        //$$     centerX, centerY, scale, centerX - mouseX, centerY - mouseY, player
        //$$ );
        //#endif
        entityRenderDispatcher.camera = oldCamera;
    }

    private static ClientLevel createFakeLevel() {
        final Minecraft minecraft = Minecraft.getInstance();
        final GameProfile nullProfile = new GameProfile(Util.NIL_UUID, "");
        //#if MC >= 1.19.2
        final WorldCreationContext context = loadDatapacks(minecraft);
        //#endif
        //#if MC >= 1.19.4
        final WorldSessionTelemetryManager worldTelemetry = new WorldSessionTelemetryManager(
            TelemetryEventSender.DISABLED, false, null
            //#if MC >= 1.20.1
            , null
            //#endif
        );
        //#endif
        return new ClientLevel(
            new ClientPacketListener(
                minecraft,
                //#if MC <= 1.20.1
                //$$ null,
                //#endif
                new Connection(PacketFlow.CLIENTBOUND),
                //#if MC >= 1.20.4
                new CommonListenerCookie(
                    nullProfile, worldTelemetry,
                    context.worldgenLoadContext(),
                    FeatureFlags.REGISTRY.allFlags(),
                    null, null, null
                    //#if MC >= 1.20.6
                    , Map.of(), null, false
                    //#endif
                    //#if MC >= 1.21
                    , Map.of(), new ServerLinks(List.of())
                    //#endif
                    //#if NEOFORGE
                    //$$ , ConnectionType.OTHER
                    //#endif
                )
                //#else
                //#if MC >= 1.19.4
                //$$ null,
                //#endif
                //$$ nullProfile,
                //#if MC >= 1.19.4
                //$$ worldTelemetry
                //#else
                //$$ minecraft.createTelemetryManager()
                //#endif
                //#endif
            ),
            new ClientLevel.ClientLevelData(Difficulty.NORMAL, false, false),
            Level.OVERWORLD,
            //#if MC >= 1.19.4
            context.worldgenLoadContext()
                .registryOrThrow(Registries.DIMENSION_TYPE)
                .getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
            //#elseif MC >= 1.19.2
            //$$ context.registryAccess()
            //$$     .registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
            //$$     .getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD),
            //#else
            //$$ RegistryAccess.BUILTIN.get()
            //$$     .registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
            //$$     .getOrCreateHolder(DimensionType.OVERWORLD_LOCATION),
            //#endif
            //#if MC >= 1.19.2
            minecraft.options.renderDistance().get(),
            minecraft.options.simulationDistance().get(),
            //#else
            //$$ minecraft.options.renderDistance,
            //$$ minecraft.options.simulationDistance,
            //#endif
            minecraft::getProfiler, minecraft.levelRenderer, false, 0L
        );
    }

    //#if MC >= 1.19.2
    private static WorldCreationContext loadDatapacks(Minecraft minecraft) {
        //#if MC >= 1.20.4
        final ServerPacksSource packsSource = new ServerPacksSource(minecraft.directoryValidator());
        //#else
        //$$ final ServerPacksSource packsSource = new ServerPacksSource();
        //#endif
        final PackRepository packRepository = new PackRepository(
            //#if MC < 1.19.4
            //$$ PackType.SERVER_DATA,
            //#endif
            packsSource
        );
        final WorldLoader.InitConfig initConfig = new WorldLoader.InitConfig(
            new WorldLoader.PackConfig(
                packRepository,
                //#if MC >= 1.19.4
                WorldDataConfiguration.DEFAULT,
                //#else
                //$$ DataPackConfig.DEFAULT,
                //#endif
                false
                //#if MC >= 1.19.4
                , false
                //#endif
            ),
            Commands.CommandSelection.INTEGRATED, 2
        );
        //#if MC >= 1.19.4
        record DataPackReloadCookie(WorldGenSettings worldGenSettings, WorldDataConfiguration dataConfiguration) {
        }
        //#endif
        try (ExecutorService gameExecutor = Executors.newSingleThreadExecutor()) {
            final CompletableFuture<WorldCreationContext> completableFuture = WorldLoader.load(
                initConfig,
                //#if MC >= 1.19.4
                dataLoadContext -> new WorldLoader.DataLoadOutput<>(
                    new DataPackReloadCookie(
                        new WorldGenSettings(
                            WorldOptions.defaultWithRandomSeed(),
                            WorldPresets.createNormalWorldDimensions(dataLoadContext.datapackWorldgen())
                        ),
                        dataLoadContext.dataConfiguration()
                    ),
                    dataLoadContext.datapackDimensions()
                ),
                //#else
                //$$ (resourceManager, dataPackConfig) -> {
                //$$     final var frozen = RegistryAccess.builtinCopy().freeze();
                //$$     final var worldGenSettings = WorldPresets.createNormalWorldFromPreset(frozen);
                //$$     return Pair.of(worldGenSettings, frozen);
                //$$ },
                //#endif
                (closeableResourceManager, reloadableServerResources, layeredRegistryAccess, object) -> {
                    closeableResourceManager.close();
                    return new WorldCreationContext(
                        //#if MC >= 1.19.4
                        object.worldGenSettings, layeredRegistryAccess, reloadableServerResources, object.dataConfiguration
                        //#else
                        //$$ object, Lifecycle.stable(), layeredRegistryAccess, reloadableServerResources
                        //#endif
                    );
                },
                Util.backgroundExecutor(), gameExecutor
            );
            return completableFuture.join();
        }
    }
    //#endif
}
