package com.reiko.cars.wash;

import com.reiko.cars.config.CarsConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A Wash Bay's pressure washer lance: hold right-click near a bay you've paid (or own) to spray a car clean (see {@link
 * WashBayService}). The bay hands it out; carried away from any bay its holder may use for {@link #RETURN_SECONDS}, it
 * goes back on the bay's holster.
 */
public class PressureWasherItem extends Item {
    static final int RETURN_SECONDS = 15;

    public PressureWasherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (WashBayService.bayFor(player) == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(WashBayService.whyNot(player), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (entity instanceof Player player && !WashBayService.spray(player)) {
            player.stopUsingItem();
            if (!level.isClientSide) {
                player.displayClientMessage(WashBayService.whyNot(player), true);
            }
        }
    }

    /** Away from any bay its holder may use, it counts down; at zero it's gone back on its holster. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || level.getGameTime() % 20 != 0 || player.isCreative()) {
            return;
        }
        long now = level.getGameTime();
        boolean usable = WashBayService.baysNear(player, CarsConfig.get(CarsConfig.LANCE_REACH) + 8.0).stream().anyMatch(bay -> bay.mayUse(player, now));
        if (usable) {
            AWAY.remove(player.getUUID());
            return;
        }
        int away = AWAY.merge(player.getUUID(), 1, Integer::sum);
        if (away >= RETURN_SECONDS) {
            AWAY.remove(player.getUUID());
            stack.setCount(0);
            player.displayClientMessage(Component.translatable("wash_bay.reikocars.lance_returned"), true);
        }
    }

    /** Seconds each player's lance has been away from any bay they may use (not saved). */
    private static final java.util.Map<java.util.UUID, Integer> AWAY = new java.util.HashMap<>();

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
        tooltip.add(Component.translatable("item.reikocars.pressure_washer.use", Math.round(CarsConfig.get(CarsConfig.LANCE_REACH)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reikocars.pressure_washer.returns").withStyle(ChatFormatting.DARK_GRAY));
    }
}
