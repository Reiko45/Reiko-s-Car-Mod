package com.reiko.cars.traffic;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A traffic light: its owner, its red-light camera's fine, and the tickets it's written. */
public final class TrafficLightBlockEntity extends OwnedRoadsideBlockEntity {
    public TrafficLightBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRAFFIC_LIGHT.get(), pos, state);
    }

    @Override
    protected long defaultFine() {
        return Math.round(CarsConfig.get(CarsConfig.RED_LIGHT_FINE) * 100.0);
    }
}
