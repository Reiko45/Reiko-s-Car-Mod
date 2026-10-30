package com.reiko.cars.road;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A street lamp's head: an arm reaching out over the road from the top of a Street Pole, with the lamp at its end. It
 * comes on at dusk and goes off at dawn (checked every couple of seconds), and stays on in the Nether and the End.
 */
public class StreetLampBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<StreetLampBlock> CODEC = simpleCodec(StreetLampBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final int CHECK_EVERY = 40;
    private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 12, 10);

    public StreetLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    /** Whether street lamps are on: after dark, and wherever there's no day. */
    public static boolean dark(Level level) {
        return !level.isDay();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The arm reaches out the way the placer faces: stand on the pavement, face the road.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(LIT, dark(context.getLevel()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        if (!level.isClientSide && !old.is(this)) {
            level.scheduleTick(pos, this, CHECK_EVERY);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = dark(level);
        if (lit != state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, lit), 3);
        }
        level.scheduleTick(pos, this, CHECK_EVERY);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
