package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.registry.ModSounds;
import com.reiko.cars.wash.CarWashBlock;
import com.reiko.cars.wash.CarWashBlockEntity;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A Car Wash's brushes, drawn from its left post's foot: two tall side brushes of blue and white foam strips that close
 * in on a car's sides, and a red and white one across the top that comes down on its roof, all spinning while it
 * washes. Its posts, beam and spray bar are block models. It also keeps the rush of water going while it washes.
 */
public final class CarWashRenderer implements BlockEntityRenderer<CarWashBlockEntity> {
    private static final ResourceLocation WHITE = ReikoCars.id("textures/entity/white.png");
    private static final int FINS = 10;
    private static final int[] SIDE_COLORS = {0x1E6FD0, 0x6FB4F0, 0xF0F4F8};
    private static final int[] TOP_COLORS = {0xD43A3A, 0xF0F4F8};
    private static final int AXLE = 0x3A3D42;
    private static final float BOTTOM = 0.12F;
    private static final float SIDE_TOP = CarWashBlock.POSTS - 0.55F;
    private static final Map<CarWashBlockEntity, WashSound> SOUNDS = new WeakHashMap<>();

    public CarWashRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CarWashBlockEntity wash, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = wash.getBlockState();
        if (!(state.getBlock() instanceof CarWashBlock)) {
            return;
        }
        keepSoundGoing(wash);
        Direction across = CarWashBlock.across(state);
        float spin = wash.spin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // Along +x is across the lane, from the left post's middle.
        poseStack.mulPose(Axis.YP.rotationDegrees(-across.toYRot() - 90.0F));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entitySolid(WHITE));
        float left = wash.leftBrush(partialTick);
        float right = wash.rightBrush(partialTick);
        float top = wash.topBrush(partialTick);
        sideBrush(poseStack, buffer, left, spin, 0.0F, light);
        sideBrush(poseStack, buffer, right, -spin, CarWashBlock.SPANS - 1, light);
        topBrush(poseStack, buffer, top, spin, light);
        poseStack.popPose();
    }

    /** A tall brush standing at {@code x} across, hung from an arm out of the post at {@code post}. */
    private static void sideBrush(PoseStack poseStack, VertexConsumer buffer, float x, float spin, float post, int light) {
        float r = CarWashBlockEntity.BRUSH_RADIUS;
        PoseStack.Pose pose = poseStack.last();
        float armY = CarWashBlock.POSTS - 0.32F;
        TollGateRenderer.box(buffer, pose, Math.min(post, x) + (post < x ? 0.12F : -0.04F), armY, -0.05F, Math.max(post, x) - (post < x ? -0.04F : 0.12F),
                armY + 0.1F, 0.05F, AXLE, light);
        TollGateRenderer.box(buffer, pose, x - 0.05F, BOTTOM, -0.05F, x + 0.05F, armY + 0.05F, 0.05F, AXLE, light);
        for (int i = 0; i < FINS; i++) {
            poseStack.pushPose();
            poseStack.translate(x, 0.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + i * 360.0F / FINS));
            // Foam strips hang a little looser at the bottom.
            TollGateRenderer.box(buffer, poseStack.last(), 0.05F, BOTTOM, -0.035F, r, SIDE_TOP, 0.035F, SIDE_COLORS[i % SIDE_COLORS.length], light);
            poseStack.popPose();
        }
    }

    /** The brush across the top, at height {@code y}, hung on rods from the beam. */
    private static void topBrush(PoseStack poseStack, VertexConsumer buffer, float y, float spin, int light) {
        float r = CarWashBlockEntity.BRUSH_RADIUS;
        float x0 = 0.75F;
        float x1 = CarWashBlock.SPANS - 1 - 0.75F;
        PoseStack.Pose pose = poseStack.last();
        for (float x : new float[] {x0 + 0.1F, x1 - 0.1F}) {
            TollGateRenderer.box(buffer, pose, x - 0.03F, y, -0.03F, x + 0.03F, CarWashBlock.POSTS + 0.1F, 0.03F, AXLE, light);
        }
        TollGateRenderer.box(buffer, pose, x0, y - 0.05F, -0.05F, x1, y + 0.05F, 0.05F, AXLE, light);
        for (int i = 0; i < FINS; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, y, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(spin + i * 360.0F / FINS));
            TollGateRenderer.box(buffer, poseStack.last(), x0 + 0.08F, 0.05F, -0.035F, x1 - 0.08F, r, 0.035F, TOP_COLORS[i % TOP_COLORS.length], light);
            poseStack.popPose();
        }
    }

    private static void keepSoundGoing(CarWashBlockEntity wash) {
        if (!wash.running()) {
            return;
        }
        WashSound sound = SOUNDS.get(wash);
        var sounds = Minecraft.getInstance().getSoundManager();
        if (sound == null || sound.isStopped() || !sounds.isActive(sound)) {
            if (sound != null) {
                sounds.stop(sound);
            }
            WashSound fresh = new WashSound(wash);
            SOUNDS.put(wash, fresh);
            sounds.play(fresh);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(CarWashBlockEntity wash) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(CarWashBlockEntity wash) {
        BlockPos pos = wash.getBlockPos();
        return new AABB(pos).inflate(CarWashBlock.SPANS, CarWashBlock.POSTS + 1, CarWashBlock.SPANS);
    }

    /** The rush of water and whirr of brushes while a Car Wash washes. */
    private static final class WashSound extends AbstractTickableSoundInstance {
        private final CarWashBlockEntity wash;

        WashSound(CarWashBlockEntity wash) {
            super(ModSounds.HOSE_SPRAY.get(), SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.wash = wash;
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.pitch = 0.85F;
            BlockPos pos = wash.getBlockPos();
            Direction across = CarWashBlock.across(wash.getBlockState());
            double middle = (CarWashBlock.SPANS - 1) / 2.0;
            this.x = pos.getX() + 0.5 + across.getStepX() * middle;
            this.y = pos.getY() + 2.0;
            this.z = pos.getZ() + 0.5 + across.getStepZ() * middle;
        }

        @Override
        public void tick() {
            if (wash.isRemoved() || !(wash.getBlockState().getBlock() instanceof CarWashBlock) || !wash.running()) {
                stop();
            }
        }
    }
}
