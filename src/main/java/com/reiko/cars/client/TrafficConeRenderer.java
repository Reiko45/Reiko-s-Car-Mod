package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.road.TrafficConeBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** A numbered traffic cone's number, on all four sides of its white band. */
public final class TrafficConeRenderer implements BlockEntityRenderer<TrafficConeBlockEntity> {
    /** The band: its middle's height, and how far its faces are from the cone's middle, in pixels. */
    private static final float BAND_Y = 5.5F;
    private static final float BAND_FACE = 3.5F;
    private final Font font;

    public TrafficConeRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(TrafficConeBlockEntity cone, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (cone.number() <= 0) {
            return;
        }
        String number = Integer.toString(cone.number());
        float scale = 2.6F / 16.0F / 7.0F; // capitals 2.6 pixels tall
        for (int side = 0; side < 4; side++) {
            poseStack.pushPose();
            poseStack.translate(0.5F, BAND_Y / 16.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F * side));
            poseStack.translate(0.0F, 0.0F, BAND_FACE / 16.0F + 0.003F);
            poseStack.scale(scale, -scale, scale);
            font.drawInBatch(number, -(font.width(number) - 1) / 2.0F, -3.5F, 0xFF111111, false, poseStack.last().pose(), buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, light);
            poseStack.popPose();
        }
    }
}
