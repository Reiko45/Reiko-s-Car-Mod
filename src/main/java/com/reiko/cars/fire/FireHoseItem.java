package com.reiko.cars.fire;

import com.reiko.cars.config.CarsConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * The fire hose: hold right-click near a fire truck to spray water from its tank (see {@link Firefighting}). The hose
 * runs from the truck's pump panel to whoever holds it, so it only reaches so far.
 */
public class FireHoseItem extends Item {
    public FireHoseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Firefighting.truckFor(player) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Firefighting.whyNot(player), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof Player player && !Firefighting.spray(player)) {
            player.stopUsingItem();
            if (!level.isClientSide) {
                player.displayClientMessage(Firefighting.whyNot(player), true);
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars.fire_hose.use", Math.round(CarsConfig.get(CarsConfig.HOSE_REACH)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reikocars.fire_hose.range", Firefighting.range()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
