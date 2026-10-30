package com.reiko.cars.car;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Headlights that light the way. In the dark (at night, in a tunnel, in a cave) a car with its engine running puts
 * two invisible lights ({@link HeadlightGlowBlock}) on the road ahead of it, a bright one about twelve blocks out and a
 * softer one about five, and moves them along as it goes; they stop short of walls, so no light leaks through into
 * a house. A police car's or ambulance's beacons, while the siren's on, light up the car itself. It's real light, so
 * it shows with any shaders and keeps mobs from spawning where it falls.
 *
 * <p>Light is slow to move (the world relights around it, and every client nearby redraws), so the lights move every
 * few ticks, and only once they're a couple of blocks behind where they should be.
 */
public final class Headlights {
    /** Block flags for putting a light in and taking it out: tell clients, but leave neighbours alone (no observer sees a car go by). */
    static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    static final int UPDATE_EVERY = 4;
    /** How far ahead of the bumper each beam's light sits, and how bright it is. */
    static final double[] REACH = {5.0, 12.0};
    static final int[] BRIGHTNESS = {13, 15};
    static final int BEACON_BRIGHTNESS = 10;
    private static final int BEACON = 2;
    private static final int SLOTS = 3;
    /** A light this close to where it should be (squared) stays put. */
    private static final double SLACK_SQ = 6.0;
    /** The lights come on below this much daylight (sky light less night's darkening) and go off again at {@link #DAYLIGHT}. */
    static final int DUSK = 9;
    static final int DAYLIGHT = 12;

    private final CarEntity car;
    private final BlockPos[] lights = new BlockPos[SLOTS];
    private boolean dark;
    private int countdown;

    Headlights(CarEntity car) {
        this.car = car;
    }

    /** On the server, every tick. */
    void tick() {
        if (--countdown > 0) {
            return;
        }
        countdown = UPDATE_EVERY;
        Level level = car.level();
        boolean on = CarsConfig.REAL_HEADLIGHTS.get();
        boolean beams = on && car.isEngineRunning() && !car.isPassenger();
        boolean beacons = on && car.sirenOn();
        BlockPos[] want = new BlockPos[SLOTS];
        int[] brightness = new int[SLOTS];
        if (beams || beacons) {
            dark = isDark(level, BlockPos.containing(car.getX(), car.getY() + 0.6, car.getZ()), dark);
            if (dark && beams) {
                for (int i = 0; i < REACH.length; i++) {
                    want[i] = beam(level, REACH[i]);
                    brightness[i] = BRIGHTNESS[i];
                }
            }
            if (dark && beacons) {
                want[BEACON] = beacon(level);
                brightness[BEACON] = BEACON_BRIGHTNESS;
            }
        }
        move(level, want, brightness);
    }

    /** Takes the car's lights away at once: it's gone, or been picked up. */
    void clear() {
        move(car.level(), new BlockPos[SLOTS], new int[SLOTS]);
    }

    /** Whether one of this car's lights is at {@code pos}. */
    boolean holds(BlockPos pos) {
        for (BlockPos light : lights) {
            if (pos.equals(light)) {
                return true;
            }
        }
        return false;
    }

    /** Whether it's dark enough for lights at {@code pos}: with some leeway either way, so they don't flicker at dusk. */
    public static boolean isDark(Level level, BlockPos pos, boolean wasDark) {
        int daylight = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken();
        return daylight < (wasDark ? DAYLIGHT : DUSK);
    }

    /** Whether some car nearby still has a light at {@code pos}. */
    static boolean owned(ServerLevel level, BlockPos pos) {
        double reach = REACH[REACH.length - 1] + 12.0;
        return !level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(reach), car -> car.headlights().holds(pos)).isEmpty();
    }

    /** Where a beam reaching {@code reach} blocks ahead lands: on the road, short of anything in the way. */
    @Nullable
    private BlockPos beam(Level level, double reach) {
        Vec3 forward = car.forward();
        Vec3 lamp = car.position().add(forward.scale(car.carType().length / 2.0)).add(0.0, 0.6, 0.0);
        Vec3 aim = lamp.add(forward.scale(reach));
        BlockHitResult hit = level.clip(new ClipContext(lamp, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, car));
        Vec3 end = aim;
        if (hit.getType() != HitResult.Type.MISS) {
            end = hit.getLocation().subtract(forward.scale(0.7));
            if (end.subtract(lamp).dot(forward) < 1.0) {
                return null; // nose up against a wall
            }
        }
        return settle(level, BlockPos.containing(end));
    }

    /** The beacons' light: just over the roof, or inside the car under a low ceiling. */
    @Nullable
    private BlockPos beacon(Level level) {
        BlockPos roof = BlockPos.containing(car.getX(), car.getY() + car.carType().height + 0.3, car.getZ());
        if (free(level, roof)) {
            return roof;
        }
        BlockPos inside = BlockPos.containing(car.getX(), car.getY() + 0.6, car.getZ());
        return free(level, inside) ? inside : null;
    }

    /** Up a step if the road climbs, and down to it if it falls away. */
    @Nullable
    private static BlockPos settle(Level level, BlockPos pos) {
        if (!free(level, pos)) {
            pos = pos.above();
            if (!free(level, pos)) {
                return null;
            }
        }
        for (int i = 0; i < 2 && free(level, pos.below()); i++) {
            pos = pos.below();
        }
        return pos;
    }

    /** Whether a light can go at {@code pos}: air, still water, or a light already. */
    static boolean free(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.getBlock() instanceof HeadlightGlowBlock || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /** Moves the lights to {@code want} (null for none), leaving any that are near enough where they are. */
    private void move(Level level, BlockPos[] want, int[] brightness) {
        if (level.isClientSide) {
            return;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (want[i] != null && lights[i] != null && lights[i].distSqr(want[i]) <= SLACK_SQ) {
                BlockState there = level.getBlockState(lights[i]);
                if (there.getBlock() instanceof HeadlightGlowBlock && there.getValue(HeadlightGlowBlock.LEVEL) == brightness[i]) {
                    want[i] = lights[i];
                }
            }
        }
        // Out with the lights that are going...
        for (BlockPos old : lights) {
            if (old != null && !contains(want, old)) {
                BlockState state = level.getBlockState(old);
                if (state.getBlock() instanceof HeadlightGlowBlock) {
                    level.setBlock(old, HeadlightGlowBlock.leaves(state), QUIET);
                }
            }
        }
        // ...and in with the new.
        BlockPos[] next = new BlockPos[SLOTS];
        for (int i = 0; i < SLOTS; i++) {
            BlockPos pos = want[i];
            if (pos == null || contains(next, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof HeadlightGlowBlock) {
                if (!contains(lights, pos)) {
                    continue; // another car's light: it lights the spot already
                }
                if (state.getValue(HeadlightGlowBlock.LEVEL) != brightness[i]) {
                    level.setBlock(pos, state.setValue(HeadlightGlowBlock.LEVEL, brightness[i]), QUIET);
                }
                next[i] = pos;
            } else if (state.isAir() || state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                level.setBlock(pos, ModBlocks.HEADLIGHT_GLOW.get().defaultBlockState()
                        .setValue(HeadlightGlowBlock.LEVEL, brightness[i])
                        .setValue(HeadlightGlowBlock.WATERLOGGED, !state.isAir()), QUIET);
                next[i] = pos;
            }
        }
        System.arraycopy(next, 0, lights, 0, SLOTS);
    }

    private static boolean contains(BlockPos[] positions, BlockPos pos) {
        for (BlockPos p : positions) {
            if (pos.equals(p)) {
                return true;
            }
        }
        return false;
    }
}
