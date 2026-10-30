package com.reiko.cars.road;

import com.reiko.cars.ReikoCars;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** The asphalt blocks' shared state: what's painted on top, and which way it's drawn. */
public final class Asphalt {
    /** Blocks cars drive faster on, with less tire wear: asphalt and its slabs and stairs. */
    public static final TagKey<Block> ROAD = BlockTags.create(ReikoCars.id("road"));
    public static final EnumProperty<RoadMarking> MARKING = EnumProperty.create("marking", RoadMarking.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private Asphalt() {
    }

    /** Which way a marking is drawn for someone looking {@code look}: arrows point the way they face. */
    public static Direction facingFor(Direction look) {
        return look.getAxis().isHorizontal() ? look : Direction.NORTH;
    }
}
