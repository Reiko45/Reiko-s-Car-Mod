package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.charge.ChargingStationBlock;
import com.reiko.cars.charge.ChargingStationBlockEntity;
import com.reiko.cars.car.FuelType;
import com.reiko.economy.api.EconomyApi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

/**
 * The lit display on the front of a charging station: the price per 1,000 MJ and how full its store is (or "NO
 * POWER"). It sits on the dark screen in the model, 15 to 21 pixels up its front, 5 pixels back from the block's edge.
 * The sign on top says "EV CHARGE".
 */
public final class ChargingStationRenderer implements BlockEntityRenderer<ChargingStationBlockEntity> {
    private static final float SCREEN_Y = 18.0F / 16.0F;
    private static final float SCREEN_DEPTH = 0.5F - 5.0F / 16.0F + 0.003F;
    private static final float SIGN_Y = (28.0F - 18.0F) / 16.0F;
    private static final int PRICE_COLOR = 0xFFFFB300;
    private static final int OK_COLOR = 0xFF4FC3F7;
    private static final int EMPTY_COLOR = 0xFFEF5350;

    private final Font font;

    public ChargingStationRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ChargingStationBlockEntity station, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light,
                       int overlay) {
        Direction facing = station.getBlockState().getValue(ChargingStationBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, SCREEN_Y, 0.5F);
        // The model faces north; turn to the station's facing, then face the text out of the front.
        poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, SCREEN_DEPTH);

        drawCentered(poseStack, buffers, EconomyApi.get().format(station.price()), 1.4F / 16.0F, 0.016F, 5.5F / 16.0F, PRICE_COLOR);
        drawCentered(poseStack, buffers, Component.translatable("display.reikocars.per_1000_mj").getString(), -0.3F / 16.0F, 0.0085F,
                5.5F / 16.0F, OK_COLOR);
        long stored = station.storedMj();
        String status = stored <= 0 ? Component.translatable("display.reikocars.no_power").getString()
                : Math.round(100.0 * stored / Math.max(1, station.capacityMj())) + "%";
        drawCentered(poseStack, buffers, status, -1.8F / 16.0F, 0.0095F, 5.5F / 16.0F, stored <= 0 ? EMPTY_COLOR : OK_COLOR);

        String sign = Component.translatable("display.reikocars.charging_station").getString();
        drawCentered(poseStack, buffers, sign, SIGN_Y, 0.017F, 9.0F / 16.0F, FuelType.ELECTRIC.signColor);
        poseStack.popPose();
    }

    /** Draws {@code text} centred on {@code y} of the screen, in blocks, shrunk to {@code maxWidth} if need be. */
    private void drawCentered(PoseStack poseStack, MultiBufferSource buffers, String text, float y, float scale, float maxWidth, int color) {
        float width = font.width(text) * scale;
        if (width > maxWidth) {
            scale *= maxWidth / width;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, y, 0.0F);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /** The station is two blocks tall, so keep drawing while its top half is on screen. */
    @Override
    public AABB getRenderBoundingBox(ChargingStationBlockEntity station) {
        return new AABB(station.getBlockPos()).expandTowards(0, 1, 0);
    }
}
