package io.github.gaming32.worldhost.gui.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.gaming32.worldhost.WHPlayerSkin;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.util.Mth;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.pose;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.Minecraft;
//#endif

//#if MC >= 1.19.4
import static com.mojang.math.Axis.*;
//#else
//$$ import static com.mojang.math.Vector3f.*;
//#endif

public class WHPlayerSkinWidget extends AbstractWidget {
    private static final float MODEL_OFFSET = 0.0625f;
    private static final float MODEL_HEIGHT = 2.125f;
    private static final float Z_OFFSET = 100f;
    private static final float ROTATION_SENSITIVITY = 2.5f;
    private static final float DEFAULT_ROTATION_X = -5f;
    private static final float DEFAULT_ROTATION_Y = 30f;
    private static final float ROTATION_X_LIMIT = 50f;

    private final Supplier<WHPlayerSkin> skin;
    private final BooleanSupplier isDeadmau5;
    private final PlayerModel<?> wideModel;
    private final PlayerModel<?> slimModel;
    private float rotationX = DEFAULT_ROTATION_X;
    private float rotationY = DEFAULT_ROTATION_Y;

    public WHPlayerSkinWidget(
        int x, int y, int width, int height,
        Supplier<WHPlayerSkin> skin, BooleanSupplier isDeadmau5,
        EntityModelSet models
    ) {
        super(x, y, width, height, Components.empty());
        this.skin = skin;
        this.isDeadmau5 = isDeadmau5;

        wideModel = new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER), false);
        slimModel = new PlayerModel<>(models.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        wideModel.young = false;
        slimModel.young = false;
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        rotationX = Mth.clamp(rotationX - (float)dragY * ROTATION_SENSITIVITY, -ROTATION_X_LIMIT, ROTATION_X_LIMIT);
        rotationY += (float)dragX * ROTATION_SENSITIVITY;
    }

    @Override
    public void playDownSound(SoundManager handler) {
    }

    @Override
    //#if MC >= 1.19.4
    public void renderWidget(
        //#else
        //$$ public void renderButton(
        //#endif
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        int mouseX, int mouseY, float partialTick
    ) {
        pose(context).pushPose();
        pose(context).translate(getX() + getWidth() / 2f, getY() + getHeight(), Z_OFFSET);
        final float scale = getHeight() / MODEL_HEIGHT;
        pose(context).scale(scale, scale, scale);
        //#if MC >= 1.20.0
        pose(context).translate(0f, -MODEL_OFFSET, 0f);
        pose(context).rotateAround(XP.rotationDegrees(rotationX), 0f, -1f - MODEL_OFFSET, 0f);
        //#else
        //$$ pose(context).translate(0f, -1f - 2 * MODEL_OFFSET, 0f);
        //$$ pose(context).mulPose(XP.rotationDegrees(rotationX));
        //$$ pose(context).translate(0f, 1f + MODEL_OFFSET, 0f);
        //#endif
        pose(context).mulPose(YP.rotationDegrees(rotationY));
        flush(context);
        Lighting.setupForEntityInInventory(
            //#if MC >= 1.20.6
            XP.rotationDegrees(rotationX)
            //#endif
        );
        renderModel(context, skin.get());
        flush(context);
        Lighting.setupFor3DItems();
        pose(context).popPose();
    }

    private void renderModel(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        WHPlayerSkin skin
    ) {
        pose(context).pushPose();
        pose(context).scale(1f, 1f, -1f);
        pose(context).translate(0f, -1.5f, 0f);
        final PlayerModel<?> model = skin.model() == WHPlayerSkin.Model.SLIM ? slimModel : wideModel;
        final var renderType = model.renderType(skin.texture());
        model.renderToBuffer(
            pose(context), bufferSource(context).getBuffer(renderType), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY
            //#if MC < 1.21
            //$$ , 1f, 1f, 1f, 1f
            //#endif
        );
        if (isDeadmau5.getAsBoolean()) {
            renderEars(context, model, skin);
        }
        if (skin.capeTexture() != null) {
            renderCape(context, model, skin);
        }
        pose(context).popPose();
    }

    private void renderEars(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        PlayerModel<?> model,
        WHPlayerSkin skin
    ) {
        final var consumer = bufferSource(context).getBuffer(RenderType.entitySolid(skin.texture()));
        for (int ear = 0; ear < 2; ear++) {
            pose(context).pushPose();
            pose(context).translate(0.375f * (ear * 2 - 1), -0.375f, 0f);
            final float scale = 1 + 1 / 3f;
            pose(context).scale(scale, scale, scale);
            model.renderEars(pose(context), consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose(context).popPose();
        }
    }

    private void renderCape(
        //#if MC < 1.20.0
        //$$ PoseStack context,
        //#else
        GuiGraphics context,
        //#endif
        PlayerModel<?> model,
        WHPlayerSkin skin
    ) {
        pose(context).pushPose();
        pose(context).translate(0f, 0f, 0.125f);
        pose(context).mulPose(XP.rotationDegrees(6f));
        pose(context).mulPose(YP.rotationDegrees(180f));
        final VertexConsumer consumer = bufferSource(context).getBuffer(RenderType.entitySolid(skin.capeTexture()));
        model.renderCloak(pose(context), consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        pose(context).popPose();
    }

    private static void flush(
        //#if MC < 1.20.0
        //$$ PoseStack context
        //#else
        GuiGraphics context
        //#endif
    ) {
        //#if MC >= 1.20.0
        context.flush();
        //#else
        //$$ RenderSystem.disableDepthTest();
        //$$ bufferSource(context).endBatch();
        //$$ RenderSystem.enableDepthTest();
        //#endif
    }

    private static MultiBufferSource.BufferSource bufferSource(
        //#if MC < 1.20.0
        //$$ PoseStack context
        //#else
        GuiGraphics context
        //#endif
    ) {
        //#if MC >= 1.20.0
        return context.bufferSource();
        //#else
        //$$ return Minecraft.getInstance().renderBuffers().bufferSource();
        //#endif
    }

    //#if MC >= 1.19.4
    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
    //#else
    //$$ @Override
    //$$ public void updateNarration(NarrationElementOutput output) {
    //$$ }
    //#endif

    //#if MC < 1.19.4
    //$$ private int getX() {
    //$$     return x;
    //$$ }
    //$$
    //$$ private int getY() {
    //$$     return y;
    //$$ }
    //#endif
}
