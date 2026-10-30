package com.reiko.cars.traffic;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shapes for roadside blocks that face one of four ways. */
final class RoadShapes {
    private RoadShapes() {
    }

    /** {@code ns} for a block facing north or south, {@code ew} for east or west. */
    static VoxelShape rotate(Direction facing, VoxelShape ns, VoxelShape ew) {
        return facing.getAxis() == Direction.Axis.Z ? ns : ew;
    }
}
