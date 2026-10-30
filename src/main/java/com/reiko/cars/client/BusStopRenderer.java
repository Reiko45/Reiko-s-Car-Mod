package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.bus.BusShelterBlock;
import com.reiko.cars.bus.BusStopBlockEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;

/**
 * A bus stop's name and routes, painted on it: on a sign, the name on the strip under its plate and the route numbers
 * on its timetable; on a shelter, the name and routes across the sign over its front.
 */
public final class BusStopRenderer implements BlockEntityRenderer<BusStopBlockEntity> {
    private final Font font;

    public BusStopRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(BusStopBlockEntity stop, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = stop.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean shelter = stop.getBlockState().getBlock() instanceof BusShelterBlock;
        String name = stop.name().isEmpty() ? stop.displayName().getString() : stop.name();
        String routes = String.join(" ", stop.routes().stream().map(route -> Integer.toString(route.route())).sorted().distinct().toList());
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        // Now the stop faces +z, and the shelter reaches along +x (the right, as you face it).
        if (shelter) {
            int lit = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
            float front = (8.0F + 1.75F) / 16.0F + 0.002F;
            String shown = routes.isEmpty() ? name.toUpperCase(java.util.Locale.ROOT) : name.toUpperCase(java.util.Locale.ROOT) + "   " + routes;
            text(pose, buffers, shown, 1.0F, 28.5F, front, 44.0F / 16.0F, 2.2F / 16.0F, 0xFFFFFFFF, lit);
            if (!routes.isEmpty()) {
                // the route numbers again, in yellow, over the white ones
                float scale = scale(shown, 44.0F / 16.0F, 2.2F / 16.0F);
                float x = 1.0F + (font.width(shown) / 2.0F - font.width(routes)) * scale;
                pose.pushPose();
                pose.translate(x, 28.5F / 16.0F, front + 0.001F);
                pose.scale(scale, -scale, scale);
                font.drawInBatch(routes, 0.0F, -font.lineHeight / 2.0F + 1.0F, 0xFFFFD54F, false, pose.last().pose(), buffers,
                        Font.DisplayMode.POLYGON_OFFSET, 0, lit);
                pose.popPose();
            }
        } else {
            text(pose, buffers, name, 0.0F, 16.375F, (8.0F - 7.1F) / 16.0F + 0.002F, 9.6F / 16.0F, 1.6F / 16.0F, 0xFF101010, light);
            if (!routes.isEmpty()) {
                text(pose, buffers, routes, 0.0F, 11.0F, (8.0F - 6.25F) / 16.0F + 0.002F, 4.2F / 16.0F, 3.0F / 16.0F, 0xFF0D47A1, light);
            }
        }
        pose.popPose();
    }

    /** The scale that fits {@code text} in {@code width} by {@code height} (blocks). */
    private float scale(String text, float width, float height) {
        return Math.min(width / Math.max(1, font.width(text)), height / (font.lineHeight - 1));
    }

    /** {@code text} centred at ({@code x} blocks across, {@code y} pixels up), on the face {@code z} out, fitted in the box. */
    private void text(PoseStack pose, MultiBufferSource buffers, String text, float x, float y, float z, float width, float height, int color, int light) {
        float scale = scale(text, width, height);
        pose.pushPose();
        pose.translate(x, y / 16.0F, z);
        pose.scale(scale, -scale, scale);
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F + 1.0F, color, false, pose.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, light);
        pose.popPose();
    }

    /** Draw a shelter's sign even when its first block is off screen. */
    @Override
    public AABB getRenderBoundingBox(BusStopBlockEntity stop) {
        return new AABB(stop.getBlockPos()).inflate(3.0);
    }
}
