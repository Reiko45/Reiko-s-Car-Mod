package com.reiko.cars.garage;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A garage door button, on a wall: put it up near your door (within {@link #REACH} blocks) and it works that door. It
 * opens or shuts it for anyone who may open the door, and for anyone at all when it's inside the garage, so nobody's
 * shut in. Its owner sneak-right-clicks it to link it to the nearest door again. No redstone.
 */
public final class GarageButtonBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<GarageButtonBlock> CODEC = simpleCodec(GarageButtonBlock::new);
    public static final BooleanProperty PRESSED = BlockStateProperties.POWERED;
    /** How far from a door a button can be and still work it, in blocks. */
    public static final double REACH = 16.0;
    private static final int PRESS_TICKS = 10;

    private static final VoxelShape NORTH = Block.box(5.0, 3.0, 14.0, 11.0, 13.0, 16.0);
    private static final VoxelShape SOUTH = Block.box(5.0, 3.0, 0.0, 11.0, 13.0, 2.0);
    private static final VoxelShape WEST = Block.box(14.0, 3.0, 5.0, 16.0, 13.0, 11.0);
    private static final VoxelShape EAST = Block.box(0.0, 3.0, 5.0, 2.0, 13.0, 11.0);

    public GarageButtonBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PRESSED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PRESSED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            default -> NORTH;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GarageButtonBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof GarageButtonBlockEntity button) {
            button.setOwner(player);
            link(player, button);
        }
    }

    /** Links the button to the nearest door within reach, and says which (or that there's none). */
    private static void link(Player player, GarageButtonBlockEntity button) {
        GarageDoorBlockEntity door = GarageDoors.nearestDoor(button.getLevel(), button.getBlockPos(), REACH);
        button.setDoor(door == null ? null : door.getBlockPos());
        player.displayClientMessage(door == null
                ? Component.translatable("garage.reikocars.button_no_door", (int) REACH)
                : Component.translatable("garage.reikocars.button_linked", GarageDoors.describe(door)), true);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PRESSED)) {
            return InteractionResult.CONSUME;
        }
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof GarageButtonBlockEntity button)) {
            return InteractionResult.SUCCESS;
        }
        press(level, pos, state);
        if (player.isSecondaryUseActive() && button.canManage(player)) {
            link(player, button);
            return InteractionResult.CONSUME;
        }
        GarageDoorBlockEntity door = button.doorEntity();
        if (door == null) {
            // Its door's gone (or was rebuilt, and its anchor moved): look for it again.
            door = GarageDoors.nearestDoor(level, pos, REACH);
            button.setDoor(door == null ? null : door.getBlockPos());
        }
        if (door == null) {
            player.displayClientMessage(Component.translatable("garage.reikocars.button_no_door", (int) REACH), true);
        } else if (GarageDoors.canOpen(player, door) || GarageDoors.depth(Vec3.atCenterOf(pos), door) < 0.0) {
            GarageDoors.toggle(door);
        } else {
            player.displayClientMessage(Component.translatable("garage.reikocars.locked", door.ownerName()), true);
        }
        return InteractionResult.CONSUME;
    }

    private static void press(Level level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(PRESSED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 0.7F);
        level.scheduleTick(pos, state.getBlock(), PRESS_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(PRESSED)) {
            level.setBlock(pos, state.setValue(PRESSED, false), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_OFF, SoundSource.BLOCKS, 0.5F, 0.6F);
        }
    }
}
