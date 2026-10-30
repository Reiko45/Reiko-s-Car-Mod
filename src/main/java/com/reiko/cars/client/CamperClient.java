package com.reiko.cars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reiko.cars.trailer.CamperLife;
import com.reiko.cars.trailer.CampLayout;
import com.reiko.cars.trailer.TrailerEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Sleeping in a camper, as the client sees it. Minecraft draws a sleeper lying the way their bed block points; a
 * camper's beds aren't blocks, so this lies them along the camper's mattress instead, holds the local sleeper there
 * (there's no bed block under them to lie on), and points their view along the bed.
 */
public final class CamperClient {
    /** Sleepers turned this frame; their ids, so the pose is put back after. */
    private static final Set<Integer> TURNED = new HashSet<>();

    private CamperClient() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CamperClient::holdSleeper);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, CamperClient::layDown);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, false, CamperClient::standUp);
        NeoForge.EVENT_BUS.addListener(CamperClient::lookAlongBed);
    }

    /** A camper and one of its beds. */
    private record InBed(TrailerEntity camper, int bed) {
        Vec3 head() {
            return CamperLife.bedSpot(camper, bed);
        }

        Vec3 feet() {
            return CamperLife.feetWay(camper, bed);
        }
    }

    /** The camper bed {@code sleeper} is asleep in, if they are: the bed at their sleeping spot nearest them. */
    @Nullable
    private static InBed bedOf(LivingEntity sleeper) {
        BlockPos at = sleeper.getSleepingPos().orElse(null);
        if (at == null || !(sleeper instanceof Player)) {
            return null;
        }
        InBed best = null;
        double nearest = Double.MAX_VALUE;
        for (TrailerEntity camper : sleeper.level().getEntitiesOfClass(TrailerEntity.class, sleeper.getBoundingBox().inflate(6.0),
                trailer -> trailer.trailerType().camper)) {
            for (int bed = 0; bed < CampLayout.beds(camper.trailerType()).size(); bed++) {
                if (CamperLife.bedPos(camper, bed).equals(at)) {
                    double distance = CamperLife.bedSpot(camper, bed).distanceToSqr(sleeper.position());
                    if (distance < nearest) {
                        nearest = distance;
                        best = new InBed(camper, bed);
                    }
                }
            }
        }
        return best;
    }

    /** Keeps the local sleeper on the mattress: with no bed block under them, they'd fall through the camper's floor. */
    private static void holdSleeper(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide || player != Minecraft.getInstance().player || !player.isSleeping()) {
            return;
        }
        InBed bed = bedOf(player);
        if (bed != null) {
            Vec3 head = bed.head();
            player.setPos(head.x, head.y, head.z);
            player.setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * Turns a sleeper drawn as Minecraft draws someone in a bed with no direction (head to the west of where they lie,
     * feet where they are) so that they lie head on the pillow, feet along the bed, as a real bed's sleeper does.
     */
    private static void layDown(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity sleeper = event.getEntity();
        if (!sleeper.isSleeping()) {
            return;
        }
        InBed bed = bedOf(sleeper);
        if (bed == null) {
            return;
        }
        Vec3 feet = bed.feet();
        float reach = sleeper.getEyeHeight(net.minecraft.world.entity.Pose.STANDING) - 0.1F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(feet.x * reach, 0.0, feet.z * reach);
        pose.mulPose(Axis.YP.rotation((float) Math.atan2(-feet.z, feet.x)));
        TURNED.add(sleeper.getId());
    }

    private static void standUp(RenderLivingEvent.Post<?, ?> event) {
        if (TURNED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    /** In first person, a camper's sleeper looks along the bed toward their feet, as in a real bed. */
    private static void lookAlongBed(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.isSleeping() || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        InBed bed = bedOf(minecraft.player);
        if (bed != null) {
            Vec3 feet = bed.feet();
            event.setYaw((float) (Mth.atan2(-feet.x, feet.z) * Mth.RAD_TO_DEG));
            event.setPitch(15.0F);
        }
    }
}
