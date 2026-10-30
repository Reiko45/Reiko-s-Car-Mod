package com.reiko.cars.weather;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.tuning.Upgrades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * What the weather does to the road under a car. Rain makes it wet ({@code weather.rainGrip} of the grip), a
 * thunderstorm worse ({@code weather.stormGrip}), and falling snow or snow underfoot worse again ({@code
 * weather.snowGrip}); ice is as slippery as its block makes it. Grip is what gets a car going, stops it and keeps it
 * pointing where it's steered, so with less of it a car takes longer to stop and slides wide in hard turns. Winter tires
 * get most of it back on snow and ice and some in the rain; off-road tires help a little in snow; sport tires are worse
 * in the wet.
 */
public final class RoadWeather {
    private RoadWeather() {
    }

    /** The state of the road under a car. */
    public enum Condition {
        DRY(ChatFormatting.GRAY),
        WET(ChatFormatting.AQUA),
        STORM(ChatFormatting.BLUE),
        SNOW(ChatFormatting.WHITE),
        ICE(ChatFormatting.AQUA);

        public final ChatFormatting color;

        Condition(ChatFormatting color) {
            this.color = color;
        }

        public Component displayName() {
            return Component.translatable("weather.reikocars.road." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    /** The road under {@code car} now. */
    public static Condition at(CarEntity car) {
        Level level = car.level();
        BlockState ground = level.getBlockState(car.getOnPos());
        BlockState here = level.getBlockState(car.blockPosition());
        if (ground.is(BlockTags.ICE)) {
            return Condition.ICE;
        }
        if (ground.is(BlockTags.SNOW) || here.is(Blocks.SNOW) || here.is(Blocks.POWDER_SNOW)) {
            return Condition.SNOW;
        }
        BlockPos top = BlockPos.containing(car.getX(), car.getY() + car.carType().height, car.getZ());
        if (!level.isRaining()) {
            return Condition.DRY;
        }
        if (snowingAt(level, top)) {
            return Condition.SNOW;
        }
        if (level.isRainingAt(top) || level.isRainingAt(top.above())) {
            return level.isThundering() ? Condition.STORM : Condition.WET;
        }
        return Condition.DRY;
    }

    /** Whether snow's falling on {@code pos}: as {@link Level#isRainingAt}, but for snow. */
    public static boolean snowingAt(Level level, BlockPos pos) {
        if (!level.isRaining() || !level.canSeeSky(pos)
                || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        }
        return level.getBiome(pos).value().getPrecipitationAt(pos) == Biome.Precipitation.SNOW;
    }

    /** Whether rain or snow is falling on {@code car}. */
    public static boolean precipitation(CarEntity car) {
        Level level = car.level();
        if (!level.isRaining()) {
            return false;
        }
        BlockPos top = BlockPos.containing(car.getX(), car.getY() + car.carType().height, car.getZ());
        return level.isRainingAt(top) || level.isRainingAt(top.above()) || snowingAt(level, top) || snowingAt(level, top.above());
    }

    /** What's left of a car's grip on {@code road} with {@code tires} (1 on a dry road). Ice is {@link #iceGrip}. */
    public static double gripFactor(Condition road, Upgrades.Tires tires) {
        double base = switch (road) {
            case WET -> CarsConfig.get(CarsConfig.RAIN_GRIP);
            case STORM -> CarsConfig.get(CarsConfig.STORM_GRIP);
            case SNOW -> CarsConfig.get(CarsConfig.SNOW_GRIP);
            default -> 1.0;
        };
        if (base >= 1.0) {
            return 1.0;
        }
        boolean snow = road == Condition.SNOW;
        // How much of the lost grip the tires win back (or, for sport tires in the wet, lose more of).
        double back = switch (tires) {
            case WINTER -> snow ? 0.8 : 0.5;
            case OFF_ROAD -> snow ? 0.35 : 0.2;
            case SPORT -> snow ? 0.0 : -0.25;
            default -> 0.0;
        };
        return Mth.clamp(base + (1.0 - base) * back, 0.1, 1.0);
    }

    /** A car's grip on a slippery block (ice: {@code surface} from the block), with winter tires' studs biting into it. */
    public static double iceGrip(double surface, Upgrades.Tires tires) {
        double gap = 1.0 - surface;
        return switch (tires) {
            case WINTER -> surface + gap * 0.45;
            case OFF_ROAD -> surface + gap * 0.12;
            default -> surface;
        };
    }
}
