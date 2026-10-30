package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.racing.RaceFlagBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** A numbered race flag's number, on the white board under the flag, both sides. */
public final class RaceFlagRenderer implements BlockEntityRenderer<RaceFlagBlockEntity> {
    private final Font font;

    public RaceFlagRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(RaceFlagBlockEntity flag, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (flag.number() <= 0) {
            return;
        }
        String number = Integer.toString(flag.number());
        float scale = Math.min(3.0F / 16.0F / 7.0F, 4.4F / 16.0F / Math.max(1, font.width(number) - 1)); // fits the 5-pixel board
        for (int side = 0; side < 2; side++) {
            poseStack.pushPose();
            // The number board: on the pole, 4 to 7.5 pixels up, its faces half a pixel either side of the middle.
            poseStack.translate(0.5F, 5.75F / 16.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F * side));
            poseStack.translate(0.0F, 0.0F, 1.0F / 16.0F + 0.003F);
            poseStack.scale(scale, -scale, scale);
            font.drawInBatch(number, -(font.width(number) - 1) / 2.0F, -3.5F, 0xFF111111, false, poseStack.last().pose(), buffers,
                    Font.DisplayMode.POLYGON_OFFSET, 0, light);
            poseStack.popPose();
        }
    }
}
