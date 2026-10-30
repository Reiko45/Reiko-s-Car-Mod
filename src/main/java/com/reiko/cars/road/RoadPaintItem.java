package com.reiko.cars.road;

import com.reiko.cars.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A can of road paint. Right-click in the air to pick the marking (sneak to go back), then right-click the top of
 * asphalt (or an asphalt slab) to paint it, drawn the way you're facing. Painting the same marking the same way again
 * scrapes it off.
 */
public class RoadPaintItem extends Item {
    public RoadPaintItem(Properties properties) {
        super(properties);
    }

    public static RoadMarking marking(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ROAD_MARKING.get(), RoadMarking.CENTER_DASHED);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        RoadMarking next = marking(stack).cycle(player.isSecondaryUseActive() ? -1 : 1);
        stack.set(ModDataComponents.ROAD_MARKING.get(), next);
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("road.reikocars.paint_set", next.displayName()), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(Asphalt.MARKING) || context.getClickedFace() != Direction.UP) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (player != null && !player.mayBuild()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        RoadMarking marking = marking(stack);
        Direction facing = Asphalt.facingFor(context.getHorizontalDirection());
        boolean scrape = state.getValue(Asphalt.MARKING) == marking && state.getValue(Asphalt.FACING) == facing;
        if (!level.isClientSide) {
            BlockState painted = scrape ? state.setValue(Asphalt.MARKING, RoadMarking.NONE)
                    : state.setValue(Asphalt.MARKING, marking).setValue(Asphalt.FACING, facing);
            level.setBlock(pos, painted, 3);
            level.playSound(null, pos, scrape ? SoundEvents.BRUSH_GRAVEL : SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.8F, scrape ? 0.8F : 1.2F);
            if (!scrape && player != null) {
                stack.hurtAndBreak(1, player, context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("road.reikocars.paint_marking", marking(stack).displayName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("road.reikocars.paint_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
