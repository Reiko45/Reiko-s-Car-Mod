package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.fire.FireHoseItem;
import com.reiko.cars.fire.Firefighting;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * The fire hoses run out from a fire truck: one from its pump panel to each player nearby with a fire hose in hand,
 * sagging between them, drawn like a lead (two crossed ribbons); and the jet of water from each one spraying, a
 * see-through ribbon along the jet's arc, widening as it goes. The truck draws them, so it's drawn whenever anyone near
 * it has a hose out, even if the truck itself is off screen.
 */
final class FireHoseLines {
    private static final int SEGMENTS = 24;
    private static final float THICKNESS = 0.045F;

    private FireHoseLines() {
    }

    /** Draws {@code truck}'s hoses, in the pose its renderer was given (the truck's position, not yet turned). */
    static void draw(CarEntity truck, float partialTick, PoseStack poseStack, MultiBufferSource buffers) {
        Level level = truck.level();
        double reach = CarsConfig.get(CarsConfig.HOSE_REACH) + 8.0;
        Vec3 origin = truck.getPosition(partialTick);
        for (Player player : level.players()) {
            if (player.isSpectator() || !holdsHose(player) || player.distanceToSqr(truck) > reach * reach || Firefighting.nearestTruck(player) != truck) {
                continue;
            }
            Vec3 hand = hand(player, partialTick);
            Vec3 outlet = Firefighting.outlet(truck, hand).subtract(truck.position()).add(origin);
            hose(poseStack, buffers, origin, outlet, hand, light(level, outlet), light(level, hand));
            if (HoseSound.spraying(player) && truck.water() > 0) {
                jet(poseStack, buffers, origin, Firefighting.trace(level, player, partialTick), hand);
            }
        }
    }

    /** Whether anyone near {@code truck} has a hose out: then it's drawn even when it's off screen, for their hose. */
    static boolean anyHoseNear(CarEntity truck) {
        double reach = CarsConfig.get(CarsConfig.HOSE_REACH) + 8.0;
        for (Player player : truck.level().players()) {
            if (holdsHose(player) && player.distanceToSqr(truck) <= reach * reach) {
                return true;
            }
        }
        return false;
    }

    /** The jet: crossed see-through ribbons along its arc, from the nozzle in the hand, widening as it goes. */
    private static void jet(PoseStack poseStack, MultiBufferSource buffers, Vec3 origin, Firefighting.Jet jet, Vec3 hand) {
        java.util.List<Vec3> points = new java.util.ArrayList<>(jet.points());
        if (points.size() < 2) {
            return;
        }
        // From the nozzle in the hand, easing onto the jet's path (which starts at the eyes) over the first few blocks.
        Vec3 nozzle = hand.add(points.get(1).subtract(points.get(0)).normalize().scale(0.4));
        Vec3 shift = nozzle.subtract(points.get(0));
        for (int i = 0; i < Math.min(5, points.size()); i++) {
            points.set(i, points.get(i).add(shift.scale(1.0 - i / 5.0)));
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f pose = poseStack.last().pose();
        int n = points.size() - 1;
        for (int i = 0; i < n; i++) {
            Vec3 a = points.get(i).subtract(origin);
            Vec3 b = points.get(i + 1).subtract(origin);
            Vec3 along = b.subtract(a);
            if (along.lengthSqr() < 1.0E-8) {
                continue;
            }
            Vec3 side = along.cross(new Vec3(0.0, 1.0, 0.0));
            side = side.lengthSqr() < 1.0E-8 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
            Vec3 up = side.cross(along).normalize();
            float wa = 0.035F + 0.22F * i / n;
            float wb = 0.035F + 0.22F * (i + 1) / n;
            float alpha = 0.62F - 0.3F * i / n;
            for (Vec3 across : new Vec3[] {side, up}) {
                quad(consumer, pose, a, b, across, wa, wb, alpha);
            }
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f pose, Vec3 a, Vec3 b, Vec3 across, float wa, float wb, float alpha) {
        float r = 0.62F;
        float g = 0.80F;
        float bl = 1.0F;
        consumer.addVertex(pose, (float) (a.x - across.x * wa), (float) (a.y - across.y * wa), (float) (a.z - across.z * wa)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (a.x + across.x * wa), (float) (a.y + across.y * wa), (float) (a.z + across.z * wa)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (b.x + across.x * wb), (float) (b.y + across.y * wb), (float) (b.z + across.z * wb)).setColor(r, g, bl, alpha);
        consumer.addVertex(pose, (float) (b.x - across.x * wb), (float) (b.y - across.y * wb), (float) (b.z - across.z * wb)).setColor(r, g, bl, alpha);
    }

    private static boolean holdsHose(Player player) {
        return player.getMainHandItem().getItem() instanceof FireHoseItem || player.getOffhandItem().getItem() instanceof FireHoseItem;
    }

    /** Roughly where the hand holding the hose is. */
    private static Vec3 hand(Player player, float partialTick) {
        boolean main = player.getMainHandItem().getItem() instanceof FireHoseItem;
        InteractionHand held = main ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        HumanoidArm arm = held == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float yaw = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0.0, -Mth.sin(yaw));
        double side = arm == HumanoidArm.RIGHT ? 0.35 : -0.35;
        return player.getPosition(partialTick).add(0.0, player.isCrouching() ? 0.75 : 0.95, 0.0).add(right.scale(side)).add(forward.scale(0.3));
    }

    private static int light(Level level, Vec3 at) {
        return LevelRenderer.getLightColor(level, BlockPos.containing(at));
    }

    private static void hose(PoseStack poseStack, MultiBufferSource buffers, Vec3 origin, Vec3 from, Vec3 to, int lightFrom, int lightTo) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.leash());
        Matrix4f pose = poseStack.last().pose();
        Vec3 start = from.subtract(origin);
        Vec3 span = to.subtract(from);
        double flat = Math.sqrt(span.x * span.x + span.z * span.z);
        float sag = (float) Math.min(1.5, 0.12 * span.length());
        float sideX = flat < 1.0E-4 ? THICKNESS : (float) (-span.z / flat * THICKNESS);
        float sideZ = flat < 1.0E-4 ? 0.0F : (float) (span.x / flat * THICKNESS);
        for (int i = 0; i <= SEGMENTS; i++) {
            vertexPair(consumer, pose, start, span, sag, i, lightFrom, lightTo, 0.0F, THICKNESS, 0.0F);
        }
        for (int i = SEGMENTS; i >= 0; i--) {
            vertexPair(consumer, pose, start, span, sag, i, lightFrom, lightTo, sideX, 0.0F, sideZ);
        }
    }

    private static void vertexPair(VertexConsumer consumer, Matrix4f pose, Vec3 start, Vec3 span, float sag, int i, int lightFrom, int lightTo,
                                   float dx, float dy, float dz) {
        float t = i / (float) SEGMENTS;
        float x = (float) (start.x + span.x * t);
        float y = (float) (start.y + span.y * t) - sag * 4.0F * t * (1.0F - t);
        float z = (float) (start.z + span.z * t);
        int block = (int) Mth.lerp(t, LightTexture.block(lightFrom), LightTexture.block(lightTo));
        int sky = (int) Mth.lerp(t, LightTexture.sky(lightFrom), LightTexture.sky(lightTo));
        int light = LightTexture.pack(block, sky);
        float shade = i % 2 == 0 ? 1.0F : 0.82F;
        float r = 0.78F * shade;
        float g = 0.13F * shade;
        float b = 0.11F * shade;
        consumer.addVertex(pose, x - dx, y - dy, z - dz).setColor(r, g, b, 1.0F).setLight(light);
        consumer.addVertex(pose, x + dx, y + dy, z + dz).setColor(r, g, b, 1.0F).setLight(light);
    }
}
