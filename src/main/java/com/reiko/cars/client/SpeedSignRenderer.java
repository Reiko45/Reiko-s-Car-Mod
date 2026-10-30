package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.traffic.SpeedSignBlock;
import com.reiko.cars.traffic.SpeedSignBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** The limit painted on a speed limit sign, under the SPEED LIMIT the model already has, with its unit beneath. */
public final class SpeedSignRenderer implements BlockEntityRenderer<SpeedSignBlockEntity> {
    /** The front of the sign's plate, in model pixels from the block's north edge (the model faces north). */
    private static final float PLATE_FRONT = 6.0F;
    private final Font font;

    public SpeedSignRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(SpeedSignBlockEntity sign, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = sign.getBlockState().getValue(SpeedSignBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // Now the sign faces +z: its plate's front is (8 - PLATE_FRONT) pixels out from the middle.
        poseStack.translate(0.0F, 0.0F, (8.0F - PLATE_FRONT) / 16.0F + 0.002F);
        String number = Integer.toString(sign.limit());
        float scale = number.length() >= 3 ? 1.0F / 52.0F : 1.0F / 40.0F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 6.5F / 16.0F, 0.0F);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(number, -font.width(number) / 2.0F, -font.lineHeight / 2.0F, 0xFF101010, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
        String unit = sign.kmh() ? "km/h" : "mph";
        float small = 1.0F / 120.0F;
        poseStack.translate(0.0F, 4.0F / 16.0F, 0.0F);
        poseStack.scale(small, -small, small);
        font.drawInBatch(unit, -font.width(unit) / 2.0F, -font.lineHeight / 2.0F, 0xFF202020, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        poseStack.popPose();
    }
}
