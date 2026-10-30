package com.reiko.cars.road;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.core.Direction;

/** A block of asphalt, which Road Paint can mark. */
public class AsphaltBlock extends Block {
    public AsphaltBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(Asphalt.MARKING, RoadMarking.NONE).setValue(Asphalt.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
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
