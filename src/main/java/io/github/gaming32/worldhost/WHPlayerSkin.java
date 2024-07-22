package io.github.gaming32.worldhost;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;


//#if MC >= 1.20.2
import net.minecraft.client.resources.PlayerSkin;
//#else
//$$ import com.mojang.authlib.minecraft.MinecraftProfileTexture;
//$$ import java.util.UUID;
//$$ import java.util.concurrent.CompletableFuture;
//$$ import net.minecraft.client.resources.DefaultPlayerSkin;
//$$ import net.minecraft.core.UUIDUtil;
//#endif

// TODO: Remove in 1.20.2+
public record WHPlayerSkin(
    ResourceLocation texture,
    ResourceLocation capeTexture,
    Model model
) {
    //#if MC >= 1.20.2
    public static WHPlayerSkin fromPlayerSkin(PlayerSkin skin) {
        return new WHPlayerSkin(skin.texture(), skin.capeTexture(), Model.byName(skin.model().id()));
    }
    //#endif

    public static WHPlayerSkin fromSkinManager(SkinManager skinManager, GameProfile profile) {
        //#if MC >= 1.20.2
        return fromPlayerSkin(skinManager.getInsecureSkin(profile));
        //#else
        //$$ final var map = skinManager.getInsecureSkinInformation(profile);
        //$$ final MinecraftProfileTexture skin = map.get(MinecraftProfileTexture.Type.SKIN);
        //$$ final ResourceLocation skinTexture;
        //$$ final String skinModel;
        //$$ if (skin != null) {
        //$$     skinTexture = skinManager.registerTexture(skin, MinecraftProfileTexture.Type.SKIN);
        //$$     skinModel = skin.getMetadata("model");
        //$$ } else {
        //$$     final UUID uuid = UUIDUtil.getOrCreatePlayerUUID(profile);
        //$$     skinTexture = DefaultPlayerSkin.getDefaultSkin(uuid);
        //$$     skinModel = DefaultPlayerSkin.getSkinModelName(uuid);
        //$$ }
        //$$ final MinecraftProfileTexture cape = map.get(MinecraftProfileTexture.Type.CAPE);
        //$$ return new WHPlayerSkin(
        //$$     skinTexture,
        //$$     cape != null ? skinTexture.registerTexture(cape, MinecraftProfileTexture.Type.CAPE) : null,
        //$$     Model.byName(skinModel)
        //$$ );
        //#endif
    }

    public enum Model {
        SLIM, WIDE;

        public static Model byName(String name) {
            return switch (name) {
                case "slim" -> SLIM;
                case null, default -> WIDE;
            };
        }
    }
}
