package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.garage.GarageDoorBlock;
import com.reiko.cars.garage.GarageDoorBlockEntity;
import com.reiko.cars.garage.GarageDoors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A finished garage door, drawn from its anchor panel. In its own frame: x runs along the door, 0 to its width, from
 * its left edge seen from outside; y up from its foot; z out of the garage, the panels' middle at 0.
 *
 * <p>A sectional door's panels run up a track and, where there's headroom inside, round a bend into the garage to lie
 * flat under the ceiling, with rails and an opener up there; without headroom they slide up into the wall. A roller
 * shutter's slats coil up into a box above the opening, inside. Both take the door's paint.
 */
public final class GarageDoorRenderer implements BlockEntityRenderer<GarageDoorBlockEntity> {
    private static final ResourceLocation SECTIONAL = ReikoCars.id("textures/entity/garage/sectional.png");
    private static final ResourceLocation SECTIONAL_BACK = ReikoCars.id("textures/entity/garage/sectional_back.png");
    private static final ResourceLocation WINDOW = ReikoCars.id("textures/entity/garage/sectional_window.png");
    private static final ResourceLocation WINDOW_BACK = ReikoCars.id("textures/entity/garage/sectional_window_back.png");
    private static final ResourceLocation GLASS = ReikoCars.id("textures/entity/garage/glass.png");
    private static final ResourceLocation SHUTTER = ReikoCars.id("textures/entity/garage/shutter.png");
    private static final ResourceLocation WHITE = ReikoCars.id("textures/entity/white.png");
    /** Half a panel's thickness, in blocks. */
    private static final float HALF = 1.5F / 16.0F;
    /** Half a shutter's thickness. */
    private static final float SHUTTER_HALF = 1.0F / 16.0F;
    /** The bend from the door's track up into the garage, over the opening. */
    private static final float BEND = 0.55F;
    private static final int RAIL = 0x3A3D42;
    private static final int OPENER = 0xE6E4DE;

    public GarageDoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** The door's frame, set up on {@code poseStack} (which starts at the anchor block's corner). */
    private static void enterFrame(GarageDoorBlockEntity door, PoseStack poseStack) {
        Direction out = GarageDoors.outward(door);
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-out.toYRot()));
        poseStack.translate(-0.5F - (door.settings().inward() ? door.width() - 1 : 0), 0.0F, 0.0F);
    }

    /** A point in the door's frame, in the world. */
    private static Vec3 toWorld(GarageDoorBlockEntity door, double x, double y, double z) {
        Direction out = GarageDoors.outward(door);
        Direction along = out.getCounterClockWise();
        Vec3 origin = Vec3.atBottomCenterOf(door.getBlockPos())
                .add(Vec3.atLowerCornerOf(along.getNormal()).scale(-0.5 - (door.settings().inward() ? door.width() - 1 : 0)));
        return origin.add(Vec3.atLowerCornerOf(along.getNormal()).scale(x)).add(0.0, y, 0.0).add(Vec3.atLowerCornerOf(out.getNormal()).scale(z));
    }

    private static int light(GarageDoorBlockEntity door, double x, double y, double z) {
        Level level = door.getLevel();
        return level == null ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, BlockPos.containing(toWorld(door, x, y, z)));
    }

    /** Whether there's room inside over the opening for the door to lie under the ceiling. */
    private static boolean headroom(GarageDoorBlockEntity door) {
        Level level = door.getLevel();
        if (level == null) {
            return false;
        }
        BlockPos above = BlockPos.containing(toWorld(door, door.width() / 2.0, door.height() + 0.5, -1.0));
        BlockState state = level.getBlockState(above);
        return !state.isSolidRender(level, above);
    }

    @Override
    public void render(GarageDoorBlockEntity door, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int overlay) {
        if (!door.isAnchor() || door.width() <= 0 || door.height() <= 0 || !(door.getBlockState().getBlock() instanceof GarageDoorBlock)) {
            return;
        }
        int rgb = door.color();
        poseStack.pushPose();
        enterFrame(door, poseStack);
        float progress = door.progress(partialTick);
        if (door.style() == GarageDoorBlock.Style.SHUTTER) {
            renderShutter(door, progress, rgb, poseStack, buffers);
        } else {
            renderSectional(door, progress, rgb, poseStack, buffers);
        }
        poseStack.popPose();
    }

    // ---- the sectional door ----

    /** Where on its track a panel's edge is, {@code t} blocks along it: {y, z}. */
    private static float[] track(float t, int height, boolean headroom) {
        if (!headroom || t <= height) {
            return new float[] {t, 0.0F};
        }
        float bend = Mth.HALF_PI * BEND;
        if (t <= height + bend) {
            float angle = (t - height) / BEND;
            return new float[] {height + BEND * Mth.sin(angle), -BEND + BEND * Mth.cos(angle)};
        }
        return new float[] {height + BEND, -BEND - (t - height - bend)};
    }

    private void renderSectional(GarageDoorBlockEntity door, float progress, int rgb, PoseStack poseStack, MultiBufferSource buffers) {
        int width = door.width();
        int height = door.height();
        boolean headroom = headroom(door);
        boolean windows = door.settings().windows() && height >= 2;
        float lift = progress * height;
        PoseStack.Pose pose = poseStack.last();
        // Where each panel is, then each texture's panels drawn together (switching buffers ends the one before).
        java.util.List<Section> panels = new java.util.ArrayList<>();
        Section windowed = null;
        for (int i = 0; i < height; i++) {
            float t0 = i + lift;
            if (!headroom && t0 >= height) {
                continue; // up in the wall
            }
            float[] bottom = track(t0, height, headroom);
            float[] top = track(t0 + 1.0F, height, headroom);
            float dy = top[0] - bottom[0];
            float dz = top[1] - bottom[1];
            float length = Mth.sqrt(dy * dy + dz * dz);
            if (length < 1.0E-4F) {
                continue;
            }
            // Its outer face's normal: out of the garage when it's upright, up when it's lying flat.
            float ny = -dz / length;
            float nz = dy / length;
            Section section = new Section(bottom[0], bottom[1], top[0], top[1], ny, nz, HALF);
            float midY = (bottom[0] + top[0]) / 2.0F;
            float midZ = (bottom[1] + top[1]) / 2.0F;
            section.light = light(door, width / 2.0, midY + ny * 0.3, midZ + nz * 0.3);
            if (windows && i == height - 1) {
                windowed = section;
            } else {
                panels.add(section);
            }
        }
        if (!panels.isEmpty()) {
            VertexConsumer front = buffers.getBuffer(RenderType.entityCutout(SECTIONAL));
            panels.forEach(section -> section.faces(front, pose, width, rgb, true));
            VertexConsumer back = buffers.getBuffer(RenderType.entityCutout(SECTIONAL_BACK));
            panels.forEach(section -> section.faces(back, pose, width, darker(rgb, 0.9F), false));
        }
        if (windowed != null) {
            windowed.faces(buffers.getBuffer(RenderType.entityCutout(WINDOW)), pose, width, rgb, true);
            windowed.faces(buffers.getBuffer(RenderType.entityCutout(WINDOW_BACK)), pose, width, darker(rgb, 0.9F), false);
            // The glass, behind the window's holes.
            Section glass = new Section(windowed.y0, windowed.z0, windowed.y1, windowed.z1, windowed.ny, windowed.nz, 0.002F);
            glass.light = windowed.light;
            VertexConsumer pane = buffers.getBuffer(RenderType.entityCutout(GLASS));
            glass.faces(pane, pose, width, 0xFFFFFF, true);
            glass.faces(pane, pose, width, 0xFFFFFF, false);
            panels.add(windowed);
        }
        VertexConsumer solid = buffers.getBuffer(RenderType.entitySolid(WHITE));
        panels.forEach(section -> section.edges(solid, pose, width, darker(rgb, 0.85F)));
        if (headroom) {
            // Its rails, up under the ceiling, and the opener at their far end.
            float railTop = height + BEND + 0.16F;
            float railBottom = height + BEND - 0.14F;
            float far = -(height + 1.0F);
            int light = light(door, width / 2.0, height + 0.5, -2.0);
            TollGateRenderer.box(solid, pose, -0.06F, railBottom, far, 0.0F, railTop, -0.5F, RAIL, light);
            TollGateRenderer.box(solid, pose, width, railBottom, far, width + 0.06F, railTop, -0.5F, RAIL, light);
            float middle = width / 2.0F;
            int openerLight = light(door, middle, height + 0.5, far - 0.3);
            TollGateRenderer.box(solid, pose, middle - 0.04F, height + 0.66F, far, middle + 0.04F, height + 0.74F, -0.5F, RAIL, openerLight);
            TollGateRenderer.box(solid, pose, middle - 0.3F, height + 0.5F, far - 0.6F, middle + 0.3F, height + 0.86F, far, OPENER, openerLight);
            TollGateRenderer.box(solid, pose, middle - 0.08F, height + 0.44F, far - 0.4F, middle + 0.08F, height + 0.5F, far - 0.2F, 0xFFF4C0,
                    LightTexture.FULL_BRIGHT);
        }
    }

    // ---- the roller shutter ----

    private void renderShutter(GarageDoorBlockEntity door, float progress, int rgb, PoseStack poseStack, MultiBufferSource buffers) {
        int width = door.width();
        int height = door.height();
        PoseStack.Pose pose = poseStack.last();
        float bottom = progress * height;
        VertexConsumer slats = buffers.getBuffer(RenderType.entityCutout(SHUTTER));
        // The curtain, a block at a time from its foot (so its slats go up with it), as far as the top of the opening.
        for (int k = 0; bottom + k < height; k++) {
            float y0 = bottom + k;
            float y1 = Math.min(height, y0 + 1.0F);
            int light = light(door, width / 2.0, (y0 + y1) / 2.0, 0.3);
            Section piece = new Section(y0, 0.0F, y1, 0.0F, 0.0F, 1.0F, SHUTTER_HALF);
            piece.vSpan = y1 - y0;
            piece.light = light;
            piece.faces(slats, pose, width, rgb, true);
            piece.faces(slats, pose, width, darker(rgb, 0.85F), false);
        }
        VertexConsumer solid = buffers.getBuffer(RenderType.entitySolid(WHITE));
        if (bottom < height) {
            // Its bottom bar.
            int light = light(door, width / 2.0, bottom + 0.1, 0.3);
            TollGateRenderer.box(solid, pose, 0.0F, bottom, -SHUTTER_HALF * 1.6F, width, bottom + 0.1F, SHUTTER_HALF * 1.6F, darker(rgb, 0.55F), light);
        }
        // Its guide rails, either side of the opening.
        int railLight = light(door, 0.5, height / 2.0, 0.3);
        TollGateRenderer.box(solid, pose, 0.0F, 0.0F, -0.1F, 0.07F, height, 0.1F, RAIL, railLight);
        TollGateRenderer.box(solid, pose, width - 0.07F, 0.0F, -0.1F, width, height, 0.1F, RAIL, railLight);
        // And the box it coils into, inside, over the opening.
        int boxLight = light(door, width / 2.0, height + 0.3, -1.0);
        TollGateRenderer.box(solid, pose, 0.0F, height - 0.02F, -1.1F, width, height + 0.58F, -0.5F, darker(rgb, 0.8F), boxLight);
    }

    private static int darker(int rgb, float by) {
        int r = Math.round(((rgb >> 16) & 255) * by);
        int g = Math.round(((rgb >> 8) & 255) * by);
        int b = Math.round((rgb & 255) * by);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * One panel (or a block of shutter): a slab from its bottom edge to its top edge across the door's width, its outer
     * face along {@code (ny, nz)}, {@code half} thick each side.
     */
    private static final class Section {
        final float y0;
        final float z0;
        final float y1;
        final float z1;
        final float ny;
        final float nz;
        final float half;
        /** How much of the texture's height it shows, from the bottom. */
        float vSpan = 1.0F;
        int light = LightTexture.FULL_BRIGHT;

        Section(float y0, float z0, float y1, float z1, float ny, float nz, float half) {
            this.y0 = y0;
            this.z0 = z0;
            this.y1 = y1;
            this.z1 = z1;
            this.ny = ny;
            this.nz = nz;
            this.half = half;
        }

        /** Its outer face ({@code front}) or inner face, a texture to each block across. */
        void faces(VertexConsumer buffer, PoseStack.Pose pose, int width, int rgb, boolean front) {
            float side = front ? half : -half;
            float by = y0 + ny * side;
            float bz = z0 + nz * side;
            float ty = y1 + ny * side;
            float tz = z1 + nz * side;
            float nY = front ? ny : -ny;
            float nZ = front ? nz : -nz;
            float vTop = 1.0F - vSpan;
            for (int x = 0; x < width; x++) {
                if (front) {
                    vertex(buffer, pose, x, by, bz, 0.0F, 1.0F, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x + 1, by, bz, 1.0F, 1.0F, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x + 1, ty, tz, 1.0F, vTop, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x, ty, tz, 0.0F, vTop, rgb, light, 0.0F, nY, nZ);
                } else {
                    vertex(buffer, pose, x + 1, by, bz, 0.0F, 1.0F, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x, by, bz, 1.0F, 1.0F, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x, ty, tz, 1.0F, vTop, rgb, light, 0.0F, nY, nZ);
                    vertex(buffer, pose, x + 1, ty, tz, 0.0F, vTop, rgb, light, 0.0F, nY, nZ);
                }
            }
        }

        /** Its edges: along the bottom and top, and its two ends. */
        void edges(VertexConsumer buffer, PoseStack.Pose pose, int width, int rgb) {
            float dy = y1 - y0;
            float dz = z1 - z0;
            float length = Mth.sqrt(dy * dy + dz * dz);
            dy /= length;
            dz /= length;
            float[] bf = {y0 + ny * half, z0 + nz * half};
            float[] bb = {y0 - ny * half, z0 - nz * half};
            float[] tf = {y1 + ny * half, z1 + nz * half};
            float[] tb = {y1 - ny * half, z1 - nz * half};
            // bottom edge, facing back down the track
            vertex(buffer, pose, 0, bf[0], bf[1], 0, 0, rgb, light, 0, -dy, -dz);
            vertex(buffer, pose, 0, bb[0], bb[1], 0, 1, rgb, light, 0, -dy, -dz);
            vertex(buffer, pose, width, bb[0], bb[1], 1, 1, rgb, light, 0, -dy, -dz);
            vertex(buffer, pose, width, bf[0], bf[1], 1, 0, rgb, light, 0, -dy, -dz);
            // top edge
            vertex(buffer, pose, 0, tf[0], tf[1], 0, 0, rgb, light, 0, dy, dz);
            vertex(buffer, pose, width, tf[0], tf[1], 1, 0, rgb, light, 0, dy, dz);
            vertex(buffer, pose, width, tb[0], tb[1], 1, 1, rgb, light, 0, dy, dz);
            vertex(buffer, pose, 0, tb[0], tb[1], 0, 1, rgb, light, 0, dy, dz);
            // the ends
            vertex(buffer, pose, 0, bb[0], bb[1], 0, 0, rgb, light, -1, 0, 0);
            vertex(buffer, pose, 0, bf[0], bf[1], 0, 1, rgb, light, -1, 0, 0);
            vertex(buffer, pose, 0, tf[0], tf[1], 1, 1, rgb, light, -1, 0, 0);
            vertex(buffer, pose, 0, tb[0], tb[1], 1, 0, rgb, light, -1, 0, 0);
            vertex(buffer, pose, width, bf[0], bf[1], 0, 0, rgb, light, 1, 0, 0);
            vertex(buffer, pose, width, bb[0], bb[1], 0, 1, rgb, light, 1, 0, 0);
            vertex(buffer, pose, width, tb[0], tb[1], 1, 1, rgb, light, 1, 0, 0);
            vertex(buffer, pose, width, tf[0], tf[1], 1, 0, rgb, light, 1, 0, 0);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int rgb, int light, float nx,
                               float ny, float nz) {
        buffer.addVertex(pose, x, y, z)
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public boolean shouldRenderOffScreen(GarageDoorBlockEntity door) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(GarageDoorBlockEntity door) {
        if (!door.isAnchor() || door.width() <= 0) {
            return new AABB(door.getBlockPos());
        }
        Vec3 a = toWorld(door, -0.5, -0.5, 1.0);
        Vec3 b = toWorld(door, door.width() + 0.5, door.height() + 1.5, -(door.height() + 2.5));
        return new AABB(a, b);
    }
}
