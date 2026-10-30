package com.reiko.cars.trailer;

import com.reiko.cars.ReikoCars;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Farm animals in a livestock trailer. They ride as its passengers, standing in pens down its length: each pen has
 * room for one horse, two cows (or sheep, pigs, goats, llamas), or four chickens or young animals. Which animals count
 * as livestock is the {@code reikocars:livestock} entity tag, so a data pack can add more.
 *
 * <p>Animals on a lead go in up the ramp: lead them onto it (while it's down), or right-click the trailer with them
 * following you. Right-click with an empty hand to let them all out down the ramp, or with a lead to take one out on
 * it.
 */
public final class Livestock {
    public static final TagKey<EntityType<?>> TAG = TagKey.create(Registries.ENTITY_TYPE, ReikoCars.id("livestock"));
    /** How far away animals on your lead can be to be loaded when you click the trailer. */
    public static final double LEAD_REACH = 10.0;
    /** Pens are four cells: front and back on the left side, front and back on the right. */
    private static final int CELLS = 4;

    private Livestock() {
    }

    /** Whether {@code entity} is a farm animal that can ride in a livestock trailer. */
    public static boolean is(Entity entity) {
        return entity instanceof Mob && entity.getType().is(TAG) && entity.isAlive();
    }

    /** How much of a pen an animal takes: a chicken or a young animal one cell, a cow two, a horse the whole pen. */
    public static int cells(Entity animal) {
        float width = animal.getBbWidth();
        return width <= 0.6F ? 1 : width <= 1.0F ? 2 : 4;
    }

    /** Where an animal stands: {@code along} pixels ahead of the trailer's middle, {@code across} pixels to its right. */
    public record Place(double along, double across) {
    }

    /**
     * Where each of {@code animals} stands, filling the pens from the front, in the same order; null if they don't all
     * fit.
     */
    @Nullable
    public static List<Place> layout(TrailerType type, List<? extends Entity> animals) {
        int rows = type.stockRows;
        boolean[] taken = new boolean[rows * CELLS];
        int[] span = type.stockSpan();
        double rowLength = span[0] / (double) rows;
        double lane = type == TrailerType.LIVESTOCK_TRAILER ? 7.0 : 8.0;
        List<Place> places = new ArrayList<>();
        for (Entity animal : animals) {
            int size = cells(animal);
            int found = -1;
            // A whole pen, a side of one (cells 0-1 or 2-3), or any one cell.
            for (int cell = 0; cell < taken.length && found < 0; cell += size) {
                boolean free = true;
                for (int i = 0; i < size; i++) {
                    free &= !taken[cell + i];
                }
                if (free) {
                    found = cell;
                }
            }
            if (found < 0) {
                return null;
            }
            for (int i = 0; i < size; i++) {
                taken[found + i] = true;
            }
            int row = found / CELLS;
            int cell = found % CELLS;
            double middle = span[1] - (row + 0.5) * rowLength;
            double across = size == 4 ? 0.0 : cell < 2 ? -lane : lane;
            double along = size == 1 ? middle + (cell % 2 == 0 ? rowLength / 4.0 : -rowLength / 4.0) : middle;
            places.add(new Place(along, across));
        }
        return places;
    }

    /** Whether {@code animal} would fit in {@code trailer} along with the animals already in it. */
    public static boolean fits(TrailerEntity trailer, Entity animal) {
        List<Entity> all = new ArrayList<>(trailer.animals());
        all.add(animal);
        return layout(trailer.trailerType(), all) != null;
    }

    /** How full the pens are, 0 to 1. */
    public static double fullness(TrailerEntity trailer) {
        int used = 0;
        for (Entity animal : trailer.animals()) {
            used += cells(animal);
        }
        return used / (double) (trailer.trailerType().stockRows * CELLS);
    }

    /** The animals on {@code player}'s leads, near enough to load. */
    public static List<Mob> ledBy(Player player) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(LEAD_REACH),
                mob -> is(mob) && !mob.isPassenger() && mob instanceof Leashable leashed && leashed.getLeashHolder() == player);
    }

    /**
     * Puts {@code animal} in the trailer, taking it off its lead (the lead goes back to whoever held it). Returns false,
     * changing nothing, if it doesn't fit.
     */
    public static boolean load(TrailerEntity trailer, Mob animal) {
        if (!trailer.trailerType().carriesLivestock() || !is(animal) || animal.isPassenger() || !fits(trailer, animal)) {
            return false;
        }
        if (animal instanceof Leashable leashed && leashed.isLeashed()) {
            Entity holder = leashed.getLeashHolder();
            leashed.dropLeash(true, false);
            if (holder instanceof Player player) {
                giveLead(player);
            } else {
                animal.spawnAtLocation(Items.LEAD);
            }
        }
        animal.ejectPassengers();
        animal.getNavigation().stop();
        if (!animal.startRiding(trailer, true)) {
            return false;
        }
        trailer.openRampFor(60);
        trailer.level().playSound(null, animal.getX(), animal.getY(), animal.getZ(), SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 1.0F, 0.8F);
        return true;
    }

    private static void giveLead(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack lead = new ItemStack(Items.LEAD);
        if (!player.getInventory().add(lead)) {
            player.drop(lead, false);
        }
    }

    /** Right-clicking a livestock trailer. */
    public static void interact(ServerPlayer player, InteractionHand hand, TrailerEntity trailer) {
        ItemStack held = player.getItemInHand(hand);
        // (not counting one just led out of this trailer: clicking again lets the rest out)
        List<Mob> following = ledBy(player).stream().filter(animal -> !trailer.justLetOut(animal)).toList();
        if (!following.isEmpty()) {
            if (trailer.isMoving()) {
                player.displayClientMessage(Component.translatable("trailer.reikocars.livestock.stop_first"), true);
                return;
            }
            int loaded = 0;
            for (Mob animal : following) {
                if (load(trailer, animal)) {
                    loaded++;
                }
            }
            player.displayClientMessage(loaded > 0
                    ? Component.translatable("trailer.reikocars.livestock.loaded", loaded, trailer.animals().size())
                    : Component.translatable("trailer.reikocars.livestock.full"), true);
            return;
        }
        List<Entity> animals = trailer.animals();
        if (animals.isEmpty()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.livestock.help"), true);
            return;
        }
        if (trailer.isMoving()) {
            player.displayClientMessage(Component.translatable("trailer.reikocars.livestock.stop_first"), true);
            return;
        }
        if (held.is(Items.LEAD)) {
            // The last one in comes out on the lead.
            Entity animal = animals.getLast();
            unload(trailer, animal, 0);
            trailer.letOut(animal);
            if (animal instanceof Leashable leashed) {
                leashed.setLeashedTo(player, true);
                held.consume(1, player);
            }
            player.displayClientMessage(Component.translatable("trailer.reikocars.livestock.led_out", animal.getDisplayName()), true);
            return;
        }
        int n = 0;
        for (Entity animal : animals) {
            unload(trailer, animal, n++);
        }
        player.displayClientMessage(Component.translatable("trailer.reikocars.livestock.unloaded", n), true);
    }

    /** Lets {@code animal} out down the ramp: the {@code n}th one out stands a little to the side of, or behind, the last. */
    public static void unload(TrailerEntity trailer, Entity animal, int n) {
        trailer.openRampFor(60);
        animal.stopRiding();
        Vec3 spot = outside(trailer, animal, n);
        animal.moveTo(spot.x, spot.y, spot.z, trailer.getYRot() + 180.0F, 0.0F);
        animal.setYHeadRot(trailer.getYRot() + 180.0F);
        trailer.level().playSound(null, spot.x, spot.y, spot.z, SoundEvents.WOOD_STEP, SoundSource.NEUTRAL, 1.0F, 1.0F);
    }

    /** Where the {@code n}th animal out stands: off the end of the ramp, three abreast, in rows. */
    public static Vec3 outside(TrailerEntity trailer, Entity animal, int n) {
        Vec3 back = trailer.forward().scale(-1.0);
        Vec3 right = trailer.right();
        double beyond = trailer.trailerType().rampLength() / 16.0 + 0.8 + (n / 3) * 1.3;
        double side = ((n % 3) - 1) * 1.1;
        Vec3 spot = trailer.rear().add(back.scale(beyond)).add(right.scale(side));
        AABB box = animal.getType().getDimensions().makeBoundingBox(spot);
        if (!trailer.level().noCollision(animal, box)) {
            Vec3 near = trailer.rear().add(back.scale(1.0));
            if (trailer.level().noCollision(animal, animal.getType().getDimensions().makeBoundingBox(near))) {
                return near;
            }
        }
        return spot;
    }

    /** Animals led onto the lowered ramp walk in. Server side, every few ticks while the ramp is down. */
    public static void loadAtRamp(TrailerEntity trailer) {
        Vec3 back = trailer.forward().scale(-1.0);
        double reach = trailer.trailerType().rampLength() / 16.0 + 1.0;
        Vec3 from = trailer.rear();
        Vec3 to = from.add(back.scale(reach));
        AABB ramp = new AABB(from, to).inflate(1.2, 0.0, 1.2).expandTowards(0.0, 2.5, 0.0).move(0.0, -0.5, 0.0);
        for (Mob animal : trailer.level().getEntitiesOfClass(Mob.class, ramp,
                mob -> is(mob) && !mob.isPassenger() && !trailer.justLetOut(mob) && mob instanceof Leashable leashed
                        && leashed.getLeashHolder() instanceof Player)) {
            Player holder = (Player) ((Leashable) animal).getLeashHolder();
            if (!trailer.canBeUsedBy(holder)) {
                continue;
            }
            if (load(trailer, animal)) {
                holder.displayClientMessage(Component.translatable("trailer.reikocars.livestock.walked_in", animal.getDisplayName(),
                        trailer.animals().size()), true);
            } else {
                holder.displayClientMessage(Component.translatable("trailer.reikocars.livestock.full"), true);
            }
        }
    }
}
