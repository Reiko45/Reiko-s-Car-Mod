package com.reiko.cars.trailer;

import com.mojang.datafixers.util.Either;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.bus.BenchSeatEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanContinueSleepingEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Living in a camper: sitting in its camp chairs, and sleeping in its beds.
 *
 * <p>Sleeping is Minecraft's own: the night passes once enough players are asleep (as the {@code playersSleepingPercentage}
 * game rule says), the screen fades, and the sleeper's spawn point is set, to the ground outside the camper's door.
 * Minecraft only lets people sleep in bed blocks, so a camper's sleepers are kept asleep here ({@link #keepAsleep}) and
 * held on the camper's mattress, and when they wake they get up and step outside.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class CamperLife {
    /** Someone asleep in a camper: in which bed, since when, and where the camper stood then. */
    public record Sleeper(int bed, long since, Vec3 camperAt) {
    }

    private CamperLife() {
    }

    // ---- camp chairs ----

    /** Sits {@code player} in camp chair {@code chair}, unless someone's in it. Server side. */
    static void sit(Player player, TrailerEntity camper, int chair) {
        if (player.isPassenger() || player.isSleeping()) {
            return;
        }
        Level level = camper.level();
        Vec3 at = camper.toWorld(CampLayout.chairs(camper.trailerType()).get(chair));
        if (!level.getEntitiesOfClass(BenchSeatEntity.class, new AABB(at, at).inflate(0.4)).isEmpty()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.camper.chair_taken"), true);
            return;
        }
        BenchSeatEntity seat = BenchSeatEntity.campChair(level, at, camper.getYRot() + 90.0F); // facing out from the camper
        level.addFreshEntity(seat);
        if (!player.startRiding(seat, true)) {
            seat.discard();
            return;
        }
        if (player instanceof ServerPlayer sitter) {
            // Sat down facing out across the camp.
            Vec3 ahead = at.add(camper.right().scale(6.0)).add(0.0, player.getEyeHeight(), 0.0);
            sitter.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, ahead);
        }
    }

    // ---- sleeping ----

    /** Where the sleeper in bed {@code bed} lies: their head, on the pillow. */
    public static Vec3 bedSpot(TrailerEntity camper, int bed) {
        return camper.toWorld(CampLayout.beds(camper.trailerType()).get(bed).head());
    }

    /** The way the sleeper in bed {@code bed}'s feet point, flat. */
    public static Vec3 feetWay(TrailerEntity camper, int bed) {
        return CampLayout.beds(camper.trailerType()).get(bed).feetBack() ? camper.forward().scale(-1.0) : camper.right();
    }

    /** The block a sleeper in bed {@code bed} counts as sleeping at, for Minecraft. */
    public static BlockPos bedPos(TrailerEntity camper, int bed) {
        return BlockPos.containing(bedSpot(camper, bed));
    }

    /** Outside the camper's door, on the ground: where sleepers get up, and their spawn point. */
    public static Vec3 door(TrailerEntity camper) {
        return camper.toWorld(CampLayout.door(camper.trailerType()));
    }

    /** {@code player} goes to bed in {@code camper}, if they can. Server side. */
    public static void sleep(ServerPlayer player, TrailerEntity camper) {
        if (!camper.trailerType().camper || !camper.isAlive()) {
            return;
        }
        if (player.isSleeping() || !player.isAlive()) {
            return;
        }
        if (camper.isPassenger()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.camper.park_to_sleep"), true);
            return;
        }
        if (!player.level().dimensionType().natural()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.camper.cant_sleep_here"), true);
            return;
        }
        int bed = freeBed(camper);
        if (bed < 0) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.camper.beds_full"), true);
            return;
        }
        // As with a bed block: trying to sleep sets your spawn, day or night.
        Vec3 door = door(camper);
        player.setRespawnPosition(player.level().dimension(), BlockPos.containing(door.x, camper.getY() + 0.1, door.z), camper.getYRot() + 90.0F,
                true, true);
        if (player.level().isDay()) {
            player.displayClientMessage(Player.BedSleepingProblem.NOT_POSSIBLE_NOW.getMessage(), true);
            return;
        }
        if (!player.isCreative()) {
            Vec3 middle = camper.position();
            List<Monster> monsters = player.level().getEntitiesOfClass(Monster.class,
                    new AABB(middle.x - 8.0, middle.y - 5.0, middle.z - 8.0, middle.x + 8.0, middle.y + 5.0, middle.z + 8.0),
                    monster -> monster.isPreventingPlayerRest(player));
            if (!monsters.isEmpty()) {
                player.displayClientMessage(Player.BedSleepingProblem.NOT_SAFE.getMessage(), true);
                return;
            }
        }
        player.closeContainer();
        player.stopRiding();
        // There's no bed block there (it's the camper's), so this skips the bed checks, and sleeps the player as a bed
        // would: the night can pass, phantoms are kept off.
        Either<Player.BedSleepingProblem, Unit> slept = player.startSleepInBed(bedPos(camper, bed));
        slept.ifLeft(problem -> {
            if (problem.getMessage() != null) {
                player.displayClientMessage(problem.getMessage(), true);
            }
        });
        if (slept.right().isPresent()) {
            camper.sleepers().put(player, new Sleeper(bed, camper.level().getGameTime(), camper.position()));
        }
    }

    /** The first bed nobody's asleep in, or -1. */
    private static int freeBed(TrailerEntity camper) {
        int beds = CampLayout.beds(camper.trailerType()).size();
        for (int bed = 0; bed < beds; bed++) {
            int b = bed;
            if (camper.sleepers().values().stream().noneMatch(sleeper -> sleeper.bed() == b)) {
                return bed;
            }
        }
        return -1;
    }

    /**
     * Each tick, on the server: holds sleepers on the mattress, wakes them if the camper's moved or hitched, and gets
     * those who've woken up out of the door.
     */
    static void tickSleepers(TrailerEntity camper, Map<Player, Sleeper> sleepers) {
        if (sleepers.isEmpty()) {
            return;
        }
        long now = camper.level().getGameTime();
        for (Map.Entry<Player, Sleeper> entry : new ArrayList<>(sleepers.entrySet())) {
            Player player = entry.getKey();
            Sleeper sleeper = entry.getValue();
            if (player.isRemoved() || player.level() != camper.level()) {
                sleepers.remove(player);
                continue;
            }
            if (player.isSleeping() && (camper.isPassenger() || camper.position().distanceToSqr(sleeper.camperAt()) > 0.01)) {
                player.stopSleepInBed(true, true); // the camper's being moved: everybody up
            }
            if (!player.isSleeping()) {
                sleepers.remove(player);
                stepOut(player, camper);
                continue;
            }
            // Minecraft lays sleepers in the middle of their bed's block; put them on the mattress. Not until the news
            // that they're asleep has gone out (at the end of the tick after they lay down), or the client would get the
            // move first and undo it.
            Vec3 spot = bedSpot(camper, sleeper.bed());
            if (now > sleeper.since() + 1 && player.position().distanceToSqr(spot) > 1.0E-4) {
                player.teleportTo(spot.x, spot.y, spot.z);
            }
        }
    }

    /** Wakes everyone asleep in {@code camper} (it's going away). */
    static void wakeAll(TrailerEntity camper) {
        for (Player player : new ArrayList<>(camper.sleepers().keySet())) {
            if (player.isSleeping()) {
                player.stopSleepInBed(true, true);
            }
            stepOut(player, camper);
        }
        camper.sleepers().clear();
    }

    /** Puts {@code player}, just up, on the ground outside the door. */
    private static void stepOut(Player player, TrailerEntity camper) {
        if (player.isRemoved() || player.isPassenger()) {
            return;
        }
        Vec3 door = door(camper);
        Vec3 at = new Vec3(door.x, camper.getY(), door.z);
        if (!player.level().noCollision(player, player.getDimensions(net.minecraft.world.entity.Pose.STANDING).makeBoundingBox(at))) {
            at = camper.rear().add(camper.forward().scale(-1.0));
        }
        player.teleportTo(at.x, at.y, at.z);
    }

    /** The camper {@code player} is asleep in, if they are. Server side. */
    @Nullable
    public static TrailerEntity camperOf(Player player) {
        if (!player.isSleeping()) {
            return null;
        }
        for (TrailerEntity camper : player.level().getEntitiesOfClass(TrailerEntity.class, player.getBoundingBox().inflate(6.0),
                trailer -> trailer.trailerType().camper)) {
            if (camper.sleepers().containsKey(player)) {
                return camper;
            }
        }
        return null;
    }

    /**
     * Minecraft wakes anyone whose sleeping spot isn't a bed; a camper's sleepers stay asleep (until morning, or until
     * something else wakes them).
     */
    @SubscribeEvent
    public static void keepAsleep(CanContinueSleepingEvent event) {
        if (event.getProblem() == Player.BedSleepingProblem.NOT_POSSIBLE_HERE && event.getEntity() instanceof Player player
                && !player.level().isClientSide && camperOf(player) != null) {
            event.setContinueSleeping(true);
        }
    }
}
