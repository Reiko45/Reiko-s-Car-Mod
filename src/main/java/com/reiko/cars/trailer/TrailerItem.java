package com.reiko.cars.trailer;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModEntities;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/**
 * A trailer, folded up. Use it on the ground to stand the trailer there on its legs, facing the way you look (so back
 * a semi up to its front to hitch it). Whoever puts it down owns it.
 */
public final class TrailerItem extends Item {
    private final TrailerType type;

    public TrailerItem(TrailerType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public TrailerType trailerType() {
        return type;
    }

    /** The trailer's paint: its dyed colour, or the type's own. */
    public static int colorOf(ItemStack stack, TrailerType type) {
        DyedItemColor dyed = stack.get(DataComponents.DYED_COLOR);
        return dyed != null ? dyed.rgb() : type.defaultColor;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        Vec3 at = ((BlockHitResult) hit).getLocation();
        TrailerEntity trailer = ModEntities.trailer(type).create(level);
        if (trailer == null) {
            return InteractionResultHolder.fail(stack);
        }
        trailer.moveTo(at.x, at.y, at.z, player.getYRot(), 0.0F);
        if (!level.noCollision(trailer, trailer.getBoundingBox())) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("car.reikocars.no_room_to_place"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            trailer.setUpFrom(stack, player);
            if (level instanceof ServerLevel serverLevel) {
                EntityType.<TrailerEntity>createDefaultStackConfig(serverLevel, stack, player).accept(trailer);
            }
            level.addFreshEntity(trailer);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars." + type.id() + ".desc").withStyle(ChatFormatting.GRAY));
        SimpleFluidContent fluid = stack.get(ModDataComponents.TANK_FLUID.get());
        if (fluid != null && !fluid.isEmpty()) {
            tooltip.add(Component.translatable("item.reikocars.tanker_trailer.holds", fluid.getAmount() / 1000, fluid.copy().getHoverName())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        CarItem.addConditionLines(tooltip, CarItem.conditionOf(stack),
                part -> part == CarCondition.Part.TIRES || part == CarCondition.Part.BRAKES || part == CarCondition.Part.BODY);
        tooltip.add(Component.translatable("item.reikocars.trailer.hitch_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
