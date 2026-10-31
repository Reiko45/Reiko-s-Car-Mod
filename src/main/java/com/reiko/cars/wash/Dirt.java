package com.reiko.cars.wash;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How dirty a car is, from 0 (clean) to 1 (filthy), and how it gets that way. Driving dirties it, by the block: hardly at
 * all on asphalt, more on other ground, much more off-road ({@link CarEntity#OFF_ROAD}) and most in mud ({@link #MUDDY}),
 * and twice as fast in the rain. Rain standing on it rinses it down to {@link #RAIN_FLOOR} (no further), and so does
 * fording water, down to {@link #WATER_FLOOR}; the Car Wash, a Wash Bay's pressure washer or a bucket of water get it
 * clean. A filthy car's body slowly rusts.
 */
public final class Dirt {
    /** Ground that's mud, or muddy: the dirtiest driving. */
    public static final TagKey<Block> MUDDY = BlockTags.create(ReikoCars.id("muddy"));
    /** Dirt added a block driven, at the normal rate. */
    static final double MUD_PER_BLOCK = 0.004;
    static final double OFF_ROAD_PER_BLOCK = 0.001;
    static final double SNOW_PER_BLOCK = 0.0004;
    static final double GROUND_PER_BLOCK = 0.000125;
    static final double ROAD_PER_BLOCK = 0.000025;
    /** Rain rinses this much off a second, but never below {@link #RAIN_FLOOR}. */
    static final float RAIN_RINSE = 0.004F;
    public static final float RAIN_FLOOR = 0.3F;
    /** Water up to the car rinses this much off a tick, but never below {@link #WATER_FLOOR}. */
    static final float WATER_RINSE = 0.01F;
    public static final float WATER_FLOOR = 0.15F;
    /** How much one bucket of water washes off by hand. */
    public static final float BUCKET_WASH = 0.34F;
    /** At this much dirt and over a car's filthy: it rusts. */
    public static final float FILTHY = 0.75F;
    private static final long DAY = 24_000;
    private static final int RUST_EVERY = 200;

    private Dirt() {
    }

    /** How a car looks for its dirt. */
    public enum Grade {
        CLEAN(ChatFormatting.GRAY),
        DUSTY(ChatFormatting.GRAY),
        DIRTY(ChatFormatting.GOLD),
        FILTHY(ChatFormatting.RED);

        public final ChatFormatting color;

        Grade(ChatFormatting color) {
            this.color = color;
        }

        public static Grade of(float dirt) {
            return dirt < 0.1F ? CLEAN : dirt < 0.35F ? DUSTY : dirt < Dirt.FILTHY ? DIRTY : FILTHY;
        }

        public Component displayName() {
            return Component.translatable("dirt.reikocars." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    /** "Dirty (52%)" and the like, coloured for how bad it is. */
    public static Component describe(float dirt) {
        Grade grade = Grade.of(dirt);
        return Component.translatable("dirt.reikocars.line", grade.displayName(), Math.round(dirt * 100.0F)).withStyle(grade.color);
    }

    /** How much dirt driving a block on {@code ground} adds, at the normal rate. */
    public static double perBlock(BlockState ground, boolean onRoad) {
        if (onRoad) {
            return ROAD_PER_BLOCK;
        }
        if (ground.is(MUDDY)) {
            return MUD_PER_BLOCK;
        }
        if (ground.is(CarEntity.OFF_ROAD)) {
            return ground.is(BlockTags.SNOW) ? SNOW_PER_BLOCK : OFF_ROAD_PER_BLOCK;
        }
        return GROUND_PER_BLOCK;
    }

    /** Whether rain's falling on {@code car}. */
    public static boolean rainedOn(CarEntity car) {
        Level level = car.level();
        BlockPos top = BlockPos.containing(car.getX(), car.getY() + car.carType().height, car.getZ());
        return level.isRainingAt(top) || level.isRainingAt(top.above());
    }

    /**
     * One server tick of a car's dirt: {@code moved} blocks driven on what's under it, rain and water rinsing it, and
     * rust once it's filthy.
     */
    public static void tick(CarEntity car, double moved, boolean driven) {
        float dirt = car.dirt();
        float before = dirt;
        double rate = CarsConfig.get(CarsConfig.DIRT_RATE);
        boolean rain = car.tickCount % 20 == 0 && rainedOn(car);
        if (driven && moved > 0.0 && rate > 0.0 && car.onGround()) {
            BlockState ground = car.level().getBlockState(car.getOnPos());
            double added = moved * perBlock(ground, car.isOnRoad()) * rate;
            if (car.level().isRaining() && car.level().isRainingAt(car.blockPosition().above())) {
                added *= 2.0; // wet ground splashes up
            }
            dirt = (float) Math.min(1.0, dirt + added);
        }
        if (car.isInWater() && dirt > WATER_FLOOR) {
            dirt = Math.max(WATER_FLOOR, dirt - WATER_RINSE);
        }
        if (rain && dirt > RAIN_FLOOR) {
            dirt = Math.max(RAIN_FLOOR, dirt - RAIN_RINSE);
        }
        if (dirt != before) {
            car.setDirt(dirt);
        }
        if (car.tickCount % RUST_EVERY == 0 && dirt >= FILTHY && !com.reiko.cars.garage.GarageDoors.garaged(car)) {
            double wear = CarsConfig.get(CarsConfig.FILTHY_BODY_WEAR) * RUST_EVERY / DAY;
            if (wear > 0.0) {
                CarCondition condition = car.condition();
                car.setCondition(condition.with(CarCondition.Part.BODY, (float) Math.max(0.0, condition.body() - wear)));
            }
        }
    }

    /** What a dealership's offer for a car comes to after its dirt: up to {@code dirtyTradeInCut} less when it's filthy. */
    public static double tradeInShare(float dirt) {
        return 1.0 - CarsConfig.get(CarsConfig.DIRTY_TRADE_IN_CUT) * Math.min(1.0F, dirt / FILTHY);
    }
}
