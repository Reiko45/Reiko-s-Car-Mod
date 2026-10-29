package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.toll.TollGateBlock;
import com.reiko.cars.toll.TollGateBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A toll gate's barrier arm, drawn from its pole: red and white stripes three and a half blocks long, with a red lamp
 * at the tip, swinging up when the gate opens and down when it shuts.
 */
public final class TollGateRenderer implements BlockEntityRenderer<TollGateBlockEntity> {
    private static final ResourceLocation WHITE = ReikoCars.id("textures/entity/white.png");
    /** The arm's length, from the pole's middle, and its cross-section, in pixels. */
    private static final float LENGTH = 56.0F;
    private static final float HEIGHT = 3.0F;
    private static final float DEPTH = 2.0F;
    /** Where it pivots: the top of the pole, in pixels. */
    private static final float PIVOT_Y = 17.0F;
    private static final int RED = 0xD32F2F;
    private static final int WHITE_PAINT = 0xF2F2EE;

    public TollGateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TollGateBlockEntity gate, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = gate.getBlockState();
        if (!(state.getBlock() instanceof TollGateBlock)) {
            return;
        }
        Direction arm = TollGateBlock.armDirection(state);
        poseStack.pushPose();
        poseStack.translate(0.5F, PIVOT_Y / 16.0F, 0.5F);
        // Face the arm along +x, then swing it up about the axis across the road.
        poseStack.mulPose(Axis.YP.rotationDegrees(-arm.toYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(gate.lift(partialTick) * 85.0F));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entitySolid(WHITE));
        PoseStack.Pose pose = poseStack.last();
        float h = HEIGHT / 32.0F;
        float d = DEPTH / 32.0F;
        int stripes = 7;
        float step = LENGTH / 16.0F / stripes;
        for (int i = 0; i < stripes; i++) {
            box(buffer, pose, i * step, -h, -d, (i + 1) * step, h, d, i % 2 == 0 ? WHITE_PAINT : RED, light);
        }
        // a lamp at the tip, and the counterweight behind the pivot
        float tip = LENGTH / 16.0F;
        box(buffer, pose, tip - 0.06F, h, -d * 0.8F, tip, h + 0.06F, d * 0.8F, 0xFF5252, light);
        box(buffer, pose, -0.28F, -h * 1.4F, -d * 1.2F, -0.06F, h * 1.4F, d * 1.2F, 0x2B2B30, light);
        poseStack.popPose();
    }

    /** A solid-coloured box from (x0, y0, z0) to (x1, y1, z1), in blocks. */
    private static void box(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        // top, bottom, and the four sides, each a little darker than the last, as the game shades faces
        quad(buffer, pose, light, r, g, b, 1.0F, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        quad(buffer, pose, light, r, g, b, 0.5F, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(buffer, pose, light, r, g, b, 0.8F, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        quad(buffer, pose, light, r, g, b, 0.8F, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
        quad(buffer, pose, light, r, g, b, 0.6F, -1, 0, 0, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
        quad(buffer, pose, light, r, g, b, 0.6F, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, int light, int r, int g, int b, float shade, float nx, float ny, float nz,
                             float... corners) {
        int sr = Math.round(r * shade);
        int sg = Math.round(g * shade);
        int sb = Math.round(b * shade);
        float[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
        for (int i = 0; i < 4; i++) {
            buffer.addVertex(pose, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2])
                    .setColor(sr, sg, sb, 255)
                    .setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(TollGateBlockEntity gate) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(TollGateBlockEntity gate) {
        return new AABB(gate.getBlockPos()).inflate(4.0);
    }
}
