package com.reiko.cars.road;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** An asphalt slab, which Road Paint can mark like the full block, for gentle ramps and kerbs. */
public class AsphaltSlabBlock extends SlabBlock {
    public AsphaltSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(Asphalt.MARKING, RoadMarking.NONE).setValue(Asphalt.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(Asphalt.MARKING, Asphalt.FACING);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(Asphalt.FACING, rotation.rotate(state.getValue(Asphalt.FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(Asphalt.FACING)));
    }
}
