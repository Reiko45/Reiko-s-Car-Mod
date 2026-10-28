package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
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
 * The lit display on the front of a pump: the price per bucket, and how much fuel is left (or "EMPTY"). It sits on
 * the dark screen in the pump's model, 19 to 26 pixels up its front.
 */
public final class GasPumpRenderer implements BlockEntityRenderer<GasPumpBlockEntity> {
    /** The screen's middle, in pixels from the bottom of the pump. */
    private static final float SCREEN_Y = 22.5F / 16.0F;
    /** How far the screen's face is from the middle of the block, toward the front. */
    private static final float SCREEN_DEPTH = 0.5F - 3.0F / 16.0F + 0.003F;
    private static final int PRICE_COLOR = 0xFFFFB300;
    private static final int OK_COLOR = 0xFF66BB6A;
    private static final int EMPTY_COLOR = 0xFFEF5350;

    private final Font font;

    public GasPumpRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(GasPumpBlockEntity pump, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = pump.getBlockState().getValue(GasPumpBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, SCREEN_Y, 0.5F);
        // The model faces north; turn to the pump's facing, then face the text out of the front.
        poseStack.mulPose(Axis.YP.rotationDegrees(-(facing.toYRot() + 180.0F)));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.0F, 0.0F, SCREEN_DEPTH);

        String price = EconomyApi.get().format(pump.price());
        drawCentered(poseStack, buffers, price, 0.0F, 1.4F / 16.0F, 0.02F, PRICE_COLOR);
        Component status = pump.fuel().isEmpty()
                ? Component.translatable("display.reikocars.empty")
                : Component.translatable("display.reikocars.per_bucket");
        drawCentered(poseStack, buffers, status.getString(), 0.0F, -1.7F / 16.0F, 0.0095F, pump.fuel().isEmpty() ? EMPTY_COLOR : OK_COLOR);
        poseStack.popPose();
    }

    /** Draws {@code text} centred on ({@code x}, {@code y}) of the screen, in blocks, shrinking it to fit if needed. */
    private void drawCentered(PoseStack poseStack, MultiBufferSource buffers, String text, float x, float y, float scale, int color) {
        float maxWidth = 9.0F / 16.0F;
        float width = font.width(text) * scale;
        if (width > maxWidth) {
            scale *= maxWidth / width;
        }
        poseStack.pushPose();
        poseStack.translate(x, y, 0.0F);
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }

    /** The pump is two blocks tall, so keep drawing while its top half is on screen. */
    @Override
    public AABB getRenderBoundingBox(GasPumpBlockEntity pump) {
        return new AABB(pump.getBlockPos()).expandTowards(0, 1, 0);
    }
}
