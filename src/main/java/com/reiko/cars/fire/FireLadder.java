package com.reiko.cars.fire;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.HeadlightGlowBlock;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The fire truck's aerial ladder. Parked, the driver's G raises it: it swings up from the roof until it stands
 * straight up behind the truck, then its four fly sections slide out, up to {@code fireTruck.ladderHeight} blocks from
 * the ground or short of whatever's above. While it's up, the column of blocks it stands in is climbable
 * ({@link FireLadderBlock}, invisible: the ladder's drawn with the truck), like scaffolding: hold jump to go up, sneak
 * to stay put. Back the truck up to a wall and the ladder runs up it to the roof. G again brings it down; the truck
 * can't be driven while it's up, and if it's moved anyway (towed, pushed) the ladder comes down.
 */
public final class FireLadder {
    public static final int RAISE_TICKS = 60;
    public static final int LOWER_TICKS = 50;
    /** The ladder swings up over this much of its travel, then the fly sections slide out over the rest. */
    public static final float SWING = 0.45F;
    public static final float EXTEND_FROM = 0.5F;
    /**
     * The ladder in model pixels: its foot's height, and how far the top fly's tip is from the foot when they're all
     * in (see the model's {@code ladder} bones). Each of the four flies slides out of the one before by up to {@link
     * #FLY_REACH}.
     */
    public static final float FOOT_Y = 45.0F;
    public static final float STOWED_REACH = 57.0F;
    public static final int FLIES = 4;
    public static final float FLY_REACH = 46.0F;
    /** It needs this many blocks of clear air behind the truck to go up: the base section alone is that tall. */
    public static final int MIN_HEIGHT = 7;
    static final int QUIET = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** The tallest the column ever is: for clearing it whatever the config says. */
    private static final int MAX_COLUMN = 17;

    private FireLadder() {
    }

    /** The driver's G in the fire truck: the ladder up, or down. */
    public static void toggle(ServerPlayer driver, CarEntity truck) {
        long now = truck.level().getGameTime();
        if (truck.ladderDeployed() && now - truck.ladderStart() < (truck.ladderUp() ? RAISE_TICKS : LOWER_TICKS)) {
            driver.displayClientMessage(Component.translatable("fire.reikocars.ladder_moving"), true);
            return;
        }
        if (truck.ladderUp()) {
            lower(truck);
            driver.displayClientMessage(Component.translatable("fire.reikocars.ladder_lowering"), true);
            return;
        }
        if (!truck.standingStill()) {
            driver.displayClientMessage(Component.translatable("fire.reikocars.ladder_stop_first"), true);
            return;
        }
        BlockPos base = base(truck);
        int height = room(truck.level(), base);
        if (height < MIN_HEIGHT) {
            driver.displayClientMessage(Component.translatable("fire.reikocars.ladder_no_room", MIN_HEIGHT), true);
            return;
        }
        raise(truck, base, height);
        driver.displayClientMessage(Component.translatable("fire.reikocars.ladder_raising", height), true);
    }

    /** Sends the ladder up from {@code base}, {@code height} blocks. */
    public static void raise(CarEntity truck, BlockPos base, int height) {
        truck.ladderBase = base;
        truck.moveLadder(true, height);
        truck.level().playSound(null, truck.getX(), truck.getY(), truck.getZ(), ModSounds.HYDRAULIC.get(), SoundSource.NEUTRAL, 1.2F, 1.1F);
    }

    /** Brings the ladder down: it can't be climbed from now on. */
    public static void lower(CarEntity truck) {
        clear(truck);
        truck.moveLadder(false, truck.ladderHeight());
        truck.level().playSound(null, truck.getX(), truck.getY(), truck.getZ(), ModSounds.HYDRAULIC.get(), SoundSource.NEUTRAL, 1.0F, 0.9F);
    }

    /** On the server, every tick (from {@link FireTrucks#serverTick}). */
    static void tick(CarEntity truck, long now) {
        long start = truck.ladderStart();
        if (start < 0) {
            return;
        }
        if (!truck.ladderUp()) {
            if (now - start >= LOWER_TICKS) {
                truck.stowLadder();
            }
            return;
        }
        BlockPos base = base(truck);
        if (truck.ladderBase == null) {
            truck.ladderBase = base; // loaded with its ladder up
        }
        if (!base.equals(truck.ladderBase) || truck.isPassenger()) {
            lower(truck); // moved with its ladder up (towed, pushed, carried off): the ladder comes down
            return;
        }
        long t = now - start;
        if (t >= RAISE_TICKS && (t == RAISE_TICKS || t % 20 == 0)) {
            place(truck);
        }
    }

    /** The block the ladder stands in at the ground: just behind the truck's back. */
    public static BlockPos base(CarEntity truck) {
        Vec3 behind = truck.position().add(truck.forward().scale(-(truck.carType().length / 2.0 + 0.45)));
        return BlockPos.containing(behind.x, truck.getY() + 0.01, behind.z);
    }

    /** How many blocks up from {@code base} the ladder can reach: as far as the config allows, short of anything solid. */
    public static int room(Level level, BlockPos base) {
        int max = CarsConfig.get(CarsConfig.LADDER_HEIGHT);
        for (int i = 0; i < max; i++) {
            BlockPos pos = base.above(i);
            if (!level.isInWorldBounds(pos) || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return i;
            }
        }
        return max;
    }

    /** Makes the raised ladder's column climbable: into air and still water, past plants and the like, up to anything solid. */
    static void place(CarEntity truck) {
        Level level = truck.level();
        BlockPos base = truck.ladderBase;
        if (base == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < truck.ladderHeight(); i++) {
            BlockPos pos = base.above(i);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FireLadderBlock) {
                continue;
            }
            boolean glow = state.getBlock() instanceof HeadlightGlowBlock;
            boolean water = glow ? state.getValue(HeadlightGlowBlock.WATERLOGGED) : state.is(Blocks.WATER) && state.getFluidState().isSource();
            if (state.isAir() || water || glow) {
                level.setBlock(pos, ModBlocks.FIRE_LADDER.get().defaultBlockState().setValue(FireLadderBlock.WATERLOGGED, water), QUIET);
            } else if (!state.getCollisionShape(level, pos).isEmpty()) {
                break; // something's been built in the way since it went up: the climb stops below it
            }
        }
    }

    /** Takes the ladder's climbable column away. */
    public static void clear(CarEntity truck) {
        Level level = truck.level();
        BlockPos base = truck.ladderBase;
        if (base == null || level.isClientSide) {
            return;
        }
        for (int i = 0; i < MAX_COLUMN; i++) {
            BlockPos pos = base.above(i);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FireLadderBlock) {
                level.setBlock(pos, FireLadderBlock.leaves(state), QUIET);
            }
        }
        truck.ladderBase = null;
    }

    /** Whether some fire truck's raised ladder stands at {@code pos}. */
    static boolean owned(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(24.0),
                truck -> truck.carType().isFireTruck() && truck.ladderUp() && holds(truck, pos)).isEmpty();
    }

    /** Whether {@code truck}'s raised ladder stands at {@code pos}. */
    public static boolean holds(CarEntity truck, BlockPos pos) {
        BlockPos base = truck.ladderBase != null ? truck.ladderBase : base(truck);
        return pos.getX() == base.getX() && pos.getZ() == base.getZ() && pos.getY() >= base.getY()
                && pos.getY() < base.getY() + truck.ladderHeight();
    }

    // ---- drawing ----

    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    /** How far up the ladder has swung, in radians, {@code out} (0 to 1) of the way through its travel. */
    public static float swing(float out) {
        return smooth(out / SWING) * Mth.HALF_PI;
    }

    /** How far each fly section has slid out of the one before, in pixels, to reach {@code height} blocks up. */
    public static float extension(float out, int height) {
        float each = Mth.clamp((height * 16.0F - FOOT_Y - STOWED_REACH) / FLIES, 0.0F, FLY_REACH);
        return each * smooth((out - EXTEND_FROM) / (1.0F - EXTEND_FROM));
    }
}
