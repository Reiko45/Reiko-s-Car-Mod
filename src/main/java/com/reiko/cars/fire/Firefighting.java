package com.reiko.cars.fire;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.job.CarJobs;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Spraying the fire hose. While someone holds it down (right-click) within {@code fireTruck.hoseLength} blocks of a
 * fire truck they may use that has water, a jet of water arcs out where they look, about twenty blocks at best. It puts
 * out fire, lit campfires and candles where it lands and along the way, puts out mobs and players on fire and gives them
 * a shove, and stings water-shy mobs (endermen, blazes, striders, snow golems). Each tick takes {@code
 * fireTruck.hoseFlow} millibuckets from the truck's tank.
 *
 * <p>Fires put out pay the Firefighter job, but not fires that never go out (on netherrack and the like), not fires
 * near where the same player lit one in the last ten minutes, and only so many a day ({@code fireTruck.paidFiresPerDay}).
 */
public final class Firefighting {
    /** The jet's speed leaving the nozzle, in blocks a tick, and how fast it falls. */
    public static final double JET_SPEED = 1.0;
    public static final double GRAVITY = 0.045;
    public static final int MAX_STEPS = 40;
    /** How far round where the jet lands it puts things out. */
    public static final double SPLASH = 1.6;
    /** How close to a fire they lit themselves a player's fires don't pay, in blocks, and for how long, in ticks. */
    static final double OWN_FIRE_RANGE = 24.0;
    static final long OWN_FIRE_TICKS = 12_000;
    private static final long DAY = 24_000;

    private Firefighting() {
    }

    // ---- which truck ----

    /** The fire truck {@code player}'s hose draws from: the nearest one in reach that has water and they may use, or null. */
    @Nullable
    public static CarEntity truckFor(Player player) {
        return trucksInReach(player).stream().filter(truck -> truck.water() > 0 && truck.canBeUsedBy(player)).findFirst().orElse(null);
    }

    /** The nearest fire truck in reach of {@code player}'s hose, water or no: the one the hose is drawn to. */
    @Nullable
    public static CarEntity nearestTruck(Player player) {
        List<CarEntity> trucks = trucksInReach(player);
        return trucks.isEmpty() ? null : trucks.getFirst();
    }

    private static List<CarEntity> trucksInReach(Player player) {
        double reach = CarsConfig.get(CarsConfig.HOSE_REACH);
        Vec3 at = player.position();
        List<CarEntity> trucks = new ArrayList<>(player.level().getEntitiesOfClass(CarEntity.class, player.getBoundingBox().inflate(reach + 4.0),
                truck -> truck.carType().isFireTruck() && truck.isAlive() && outlet(truck, at).distanceTo(at) <= reach));
        trucks.sort(Comparator.comparingDouble(truck -> outlet(truck, at).distanceToSqr(at)));
        return trucks;
    }

    /** Why {@code player}'s hose won't spray: no truck in reach, the one in reach is locked, or it's out of water. */
    public static Component whyNot(Player player) {
        List<CarEntity> trucks = trucksInReach(player);
        if (trucks.isEmpty()) {
            return Component.translatable("fire.reikocars.no_truck", Math.round(CarsConfig.get(CarsConfig.HOSE_REACH)));
        }
        if (trucks.stream().noneMatch(truck -> truck.canBeUsedBy(player))) {
            return Component.translatable("fire.reikocars.truck_locked");
        }
        return Component.translatable("fire.reikocars.tank_empty");
    }

    /**
     * Where the hose comes off the truck: the pump panel behind the cab, on whichever side {@code toward} is. (The
     * panel's 10 pixels behind the truck's middle less 0.6 blocks; its outlets stand out 19.5 pixels.)
     */
    public static Vec3 outlet(CarEntity truck, Vec3 toward) {
        Vec3 forward = truck.forward();
        Vec3 left = new Vec3(forward.z, 0.0, -forward.x);
        Vec3 middle = truck.position().add(forward.scale(-0.6));
        double side = toward.subtract(middle).dot(left) >= 0 ? 1.0 : -1.0;
        return middle.add(left.scale(side * 19.5 / 16.0)).add(0.0, 1.2, 0.0);
    }

    // ---- the jet ----

    /** The jet's path, from the nozzle on, and where it lands (null if it's still in the air at the end). */
    public record Jet(List<Vec3> points, @Nullable Vec3 impact, @Nullable Direction face) {
    }

    /** Where the water goes when {@code player} sprays where they're looking. */
    public static Jet trace(Level level, Player player, float partialTick) {
        Vec3 look = player.getViewVector(partialTick);
        Vec3 pos = player.getEyePosition(partialTick).add(look.scale(0.6)).add(0.0, -0.3, 0.0);
        Vec3 velocity = look.scale(JET_SPEED);
        List<Vec3> points = new ArrayList<>();
        points.add(pos);
        for (int i = 0; i < MAX_STEPS; i++) {
            Vec3 next = pos.add(velocity);
            BlockHitResult hit = level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
            if (hit.getType() != HitResult.Type.MISS) {
                points.add(hit.getLocation());
                return new Jet(points, hit.getLocation(), hit.getDirection());
            }
            points.add(next);
            pos = next;
            velocity = velocity.add(0.0, -GRAVITY, 0.0).scale(0.99);
        }
        return new Jet(points, null, null);
    }

    /**
     * One tick of spraying, on either side: false once there's no truck in reach with water for it. On the server
     * it takes the water and puts things out; on a client it draws the spray.
     */
    public static boolean spray(Player player) {
        CarEntity truck = truckFor(player);
        if (truck == null) {
            return false;
        }
        if (player instanceof ServerPlayer server) {
            truck.setWater(truck.water() - CarsConfig.get(CarsConfig.HOSE_FLOW));
            douse(server, trace(server.level(), server, 1.0F));
        } else if (player.level().isClientSide) {
            drawSpray(player.level(), trace(player.level(), player, 1.0F), player.getRandom());
        }
        return true;
    }

    /** The jet as a client sees it, besides the jet itself (drawn with the truck): drops falling from it, and a splash where it lands. */
    public static void drawSpray(Level level, Jet jet, net.minecraft.util.RandomSource random) {
        List<Vec3> points = jet.points();
        for (int i = 3; i < points.size(); i += 2) {
            if (random.nextInt(3) == 0) {
                Vec3 at = points.get(i);
                double spread = 0.05 + 0.01 * i;
                level.addParticle(ParticleTypes.FALLING_WATER, at.x + random.nextGaussian() * spread, at.y - 0.1, at.z + random.nextGaussian() * spread,
                        0.0, 0.0, 0.0);
            }
        }
        if (jet.impact() != null) {
            Vec3 at = jet.impact();
            for (int k = 0; k < 8; k++) {
                level.addParticle(ParticleTypes.SPLASH, at.x + random.nextGaussian() * 0.45, at.y + 0.1, at.z + random.nextGaussian() * 0.45,
                        random.nextGaussian() * 0.1, 0.2 + random.nextDouble() * 0.2, random.nextGaussian() * 0.1);
            }
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.CLOUD, at.x, at.y + 0.2, at.z, 0.0, 0.03, 0.0);
            }
        }
    }

    /** Puts out what the jet passes through and lands on. Returns how many fires it put out. */
    public static int douse(ServerPlayer player, Jet jet) {
        ServerLevel level = player.serverLevel();
        Set<BlockPos> wet = new HashSet<>();
        Set<Entity> soaked = new HashSet<>();
        List<Vec3> points = jet.points();
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            int samples = Math.max(1, (int) Math.ceil(from.distanceTo(to) / 0.5));
            for (int s = 1; s <= samples; s++) {
                wet.add(BlockPos.containing(from.lerp(to, s / (double) samples)));
            }
            soaked.addAll(level.getEntities(player, new AABB(from, to).inflate(0.4), entity -> entity instanceof LivingEntity && entity.isAlive()
                    && !entity.isSpectator()));
        }
        if (jet.impact() != null) {
            Vec3 at = jet.impact();
            BlockPos centre = BlockPos.containing(at);
            int r = (int) Math.ceil(SPLASH);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-r, -r, -r), centre.offset(r, r, r))) {
                if (pos.getCenter().distanceTo(at) <= SPLASH + 0.5) {
                    wet.add(pos.immutable());
                }
            }
            soaked.addAll(level.getEntities(player, new AABB(at, at).inflate(SPLASH), entity -> entity instanceof LivingEntity && entity.isAlive()
                    && !entity.isSpectator()));
        }
        int fires = 0;
        for (BlockPos pos : wet) {
            if (douseBlock(level, pos, player)) {
                fires++;
            }
        }
        Vec3 direction = points.size() > 1 ? points.get(points.size() - 1).subtract(points.get(0)).normalize() : player.getLookAngle();
        for (Entity entity : soaked) {
            douseEntity(level, (LivingEntity) entity, direction, player);
        }
        return fires;
    }

    /** Puts out a fire, lit campfire or lit candle at {@code pos}. Returns whether it was a fire. */
    static boolean douseBlock(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.FIRE)) {
            BlockState below = level.getBlockState(pos.below());
            // A fire that never goes out by itself: on netherrack (the dimension's endless-burning blocks), or soul fire.
            boolean endless = state.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE) || below.isFireSource(level, pos.below(), Direction.UP);
            level.removeBlock(pos, false);
            level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            if (!endless) {
                credit(player, pos);
            }
            return true;
        }
        if (AbstractCandleBlock.isLit(state)) {
            AbstractCandleBlock.extinguish(null, state, level, pos);
        } else if (CampfireBlock.isLitCampfire(state)) {
            level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            CampfireBlock.dowse(player, level, pos, state);
            level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        }
        return false;
    }

    /** Puts out {@code entity} if it's burning, pushes it along a little, and stings it if water hurts it. */
    static void douseEntity(ServerLevel level, LivingEntity entity, Vec3 direction, ServerPlayer player) {
        if (entity.isOnFire()) {
            entity.clearFire();
            level.sendParticles(ParticleTypes.SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
        }
        if (entity.isSensitiveToWater() && level.getGameTime() % 10 == 0) {
            entity.hurt(level.damageSources().indirectMagic(player, player), 1.0F);
        }
        if (entity != player.getVehicle() && !entity.isPassenger()) {
            entity.push(direction.x * 0.05, 0.01, direction.z * 0.05);
            if (entity instanceof ServerPlayer) {
                entity.hurtMarked = true; // so the shove reaches them
            }
        }
    }

    // ---- the Firefighter job ----

    /** Fires lit by each player recently: they don't pay to put out. */
    private static final Map<UUID, Deque<Lit>> LIT = new HashMap<>();
    /** Fires paid for today, per player: the day and the count. */
    private static final Map<UUID, long[]> PAID = new HashMap<>();

    private record Lit(ResourceKey<Level> dimension, BlockPos pos, long time) {
    }

    /** {@code player} lit a fire (or poured lava, or threw a fire charge) at {@code pos}. */
    public static void lit(ServerPlayer player, BlockPos pos) {
        long now = player.serverLevel().getGameTime();
        Deque<Lit> fires = LIT.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        while (!fires.isEmpty() && now - fires.peekFirst().time() > OWN_FIRE_TICKS) {
            fires.pollFirst();
        }
        fires.addLast(new Lit(player.level().dimension(), pos.immutable(), now));
        while (fires.size() > 64) {
            fires.pollFirst();
        }
    }

    /** Whether {@code player} lit a fire near {@code pos} in the last ten minutes. */
    public static boolean litNear(ServerPlayer player, BlockPos pos) {
        Deque<Lit> fires = LIT.get(player.getUUID());
        if (fires == null) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        double range = OWN_FIRE_RANGE * OWN_FIRE_RANGE;
        return fires.stream().anyMatch(fire -> now - fire.time() <= OWN_FIRE_TICKS && fire.dimension() == player.level().dimension()
                && fire.pos().distSqr(pos) <= range);
    }

    /** A fire put out by {@code player} at {@code pos}: it pays, unless they lit it or have been paid enough today. */
    static void credit(ServerPlayer player, BlockPos pos) {
        if (!CarJobs.paid(CarJobs.FIRE_PUT_OUT) || litNear(player, pos)) {
            return;
        }
        long day = player.serverLevel().getGameTime() / DAY;
        long[] paid = PAID.computeIfAbsent(player.getUUID(), id -> new long[] {day, 0});
        if (paid[0] != day) {
            paid[0] = day;
            paid[1] = 0;
        }
        if (paid[1] >= CarsConfig.get(CarsConfig.PAID_FIRES_PER_DAY)) {
            return;
        }
        paid[1]++;
        CarJobs.work(player, CarJobs.FIRE_PUT_OUT, 1);
    }

    /** Forgets who lit what and who's been paid: for tests. */
    public static void forget(UUID player) {
        LIT.remove(player);
        PAID.remove(player);
    }

    /** About how far the jet reaches at best, in blocks: for the tooltip. */
    public static int range() {
        return Mth.floor(JET_SPEED * JET_SPEED / GRAVITY);
    }
}
