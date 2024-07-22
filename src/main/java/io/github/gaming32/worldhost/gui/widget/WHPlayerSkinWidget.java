package io.github.gaming32.worldhost.gui.widget;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import io.github.gaming32.worldhost.WHPlayerSkin;
import io.github.gaming32.worldhost.versions.Components;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;

import java.util.function.Supplier;

import static io.github.gaming32.worldhost.gui.screen.WorldHostScreen.pose;

//#if MC >= 1.20.0
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
//#else
//$$ import com.mojang.blaze3d.systems.RenderSystem;
//$$ import com.mojang.blaze3d.vertex.PoseStack;
//$$ import net.minecraft.client.Minecraft;
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
    private final PlayerModel<?> wideModel;
    private final PlayerModel<?> slimModel;
    private float rotationX = DEFAULT_ROTATION_X;
    private float rotationY = DEFAULT_ROTATION_Y;

    public WHPlayerSkinWidget(
        int x, int y, int width, int height,
        Supplier<WHPlayerSkin> skin,
        EntityModelSet models
    ) {
        super(x, y, width, height, Components.empty());
        this.skin = skin;

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
        pose(context).translate(0f, -MODEL_OFFSET, 0f);
        pose(context).rotateAround(Axis.XP.rotationDegrees(rotationX), 0f, -1f - MODEL_OFFSET, 0f);
        pose(context).mulPose(Axis.YP.rotationDegrees(rotationY));
        flush(context);
        Lighting.setupForEntityInInventory(Axis.XP.rotationDegrees(rotationX));
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
        model.renderToBuffer(pose(context), bufferSource(context).getBuffer(renderType), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
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
    //$$
    //$$ private int getWidth() {
    //$$     return width;
    //$$ }
    //$$
    //$$ private int getHeight() {
    //$$     return height;
    //$$ }
    //#endif
}
