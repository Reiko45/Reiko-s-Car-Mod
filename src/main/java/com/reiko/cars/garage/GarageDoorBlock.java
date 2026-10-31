package com.reiko.cars.garage;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A garage door panel. Put panels side by side and one on another to fill a doorway: touching panels of one owner,
 * facing the same way, join into one door once they make a rectangle (up to {@link GarageDoors#maxWidth()} wide and
 * {@link GarageDoors#maxHeight()} tall). Its bottom left panel, seen from outside, is the door's {@link Part#ANCHOR}: it
 * keeps the door's state and draws the whole door; the others are {@link Part#MEMBER}s. A panel that isn't part of a
 * finished door is {@link Part#SINGLE}, and just stands there.
 *
 * <p>A panel faces outward ({@link #FACING}). {@link #OPEN} is whether its row of the doorway is clear: rows clear from
 * the bottom up as the door rises, and close again from the bottom as it comes down.
 */
public final class GarageDoorBlock extends BaseEntityBlock {
    /** The two kinds of door. */
    public enum Style implements StringRepresentable {
        /** A house's sectional door: panels that roll up and back under the ceiling, windows along the top. */
        SECTIONAL(0xF2F1EC),
        /** A metal roller shutter: slats that roll up into the lintel. */
        SHUTTER(0xB9BEC4);

        public static final com.mojang.serialization.Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);
        /** Its paint until it's dyed. */
        public final int defaultColor;

        Style(int defaultColor) {
            this.defaultColor = defaultColor;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** What part of a door a panel is. */
    public enum Part implements StringRepresentable {
        SINGLE,
        ANCHOR,
        MEMBER;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public static final MapCodec<GarageDoorBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Style.CODEC.fieldOf("style").forGetter(GarageDoorBlock::style),
            propertiesCodec()
    ).apply(instance, GarageDoorBlock::new));

    /** The panel: 3 pixels thick, in the middle of the block. */
    private static final VoxelShape ACROSS_X = Block.box(0.0, 0.0, 6.5, 16.0, 16.0, 9.5);
    private static final VoxelShape ACROSS_Z = Block.box(6.5, 0.0, 0.0, 9.5, 16.0, 16.0);

    private final Style style;

    public GarageDoorBlock(Style style, BlockBehaviour.Properties properties) {
        super(properties);
        this.style = style;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(PART, Part.SINGLE).setValue(OPEN, false));
    }

    public Style style() {
        return style;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OPEN);
    }

    /** The panel's slab, whichever way it faces. */
    public static VoxelShape slab(BlockState state) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ACROSS_X : ACROSS_Z;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(OPEN)) {
            return slab(state);
        }
        // Rolled up: what's left to click (to close it) is the top row, where the door is now.
        return level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel && panel.isTopRow() ? slab(state) : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : slab(state);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // a finished door's panels have an empty model: its anchor draws it
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Facing out toward whoever puts it up, unless it's going next to a panel of this kind: then the way that faces,
        // so a door built from inside still faces the same way all over.
        Direction facing = context.getHorizontalDirection().getOpposite();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction side : Direction.values()) {
            BlockState next = level.getBlockState(pos.relative(side));
            if (next.is(this) && side.getAxis() != next.getValue(FACING).getAxis()) {
                facing = next.getValue(FACING);
                break;
            }
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel && level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
            if (placer instanceof Player player) {
                panel.setOwner(player);
            }
            GarageDoors.Joined joined = GarageDoors.rejoin(serverLevel, pos);
            if (placer instanceof Player player && joined != null) {
                player.displayClientMessage(joined.message(), true);
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            return; // the same panel, only its part or how open it is has changed
        }
        GarageDoorBlockEntity removed = level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel ? panel : null;
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            GarageDoors.afterRemoved(serverLevel, pos, state, removed); // (its records go to what's left of its door)
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dye) {
            if (!level.isClientSide) {
                GarageDoors.paint(player, pos, dye, stack);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            GarageDoors.click(player, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GarageDoorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (state.getValue(PART) != Part.ANCHOR) {
            return null; // only a door's anchor has anything to do
        }
        return level.isClientSide
                ? createTickerHelper(type, ModBlockEntities.GARAGE_DOOR.get(), (lvl, pos, st, door) -> door.clientTick())
                : createTickerHelper(type, ModBlockEntities.GARAGE_DOOR.get(), (lvl, pos, st, door) -> GarageDoors.serverTick((ServerLevel) lvl, door));
    }

    /** The door's name for messages: "Garage Door" or "Roller Shutter". */
    public Component doorName() {
        return getName();
    }
}
