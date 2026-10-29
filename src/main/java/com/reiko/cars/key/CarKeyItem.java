package com.reiko.cars.key;

import com.reiko.cars.registry.ModDataComponents;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** A key cut for one car or trailer (see {@link CarKeys}). Right-click to lock or unlock it from a distance. */
public final class CarKeyItem extends Item {
    public CarKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CarKey key = stack.get(ModDataComponents.CAR_KEY.get());
        return key == null ? super.getName(stack) : Component.translatable("item.reikocars.car_key.of", Component.translatable(key.vehicle()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            CarKeys.fob(serverPlayer, stack);
        }
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CarKey key = stack.get(ModDataComponents.CAR_KEY.get());
        if (key != null && !key.owner().isEmpty()) {
            tooltip.add(Component.translatable("item.reikocars.car_key.owner", key.owner()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.reikocars.car_key.fob", (int) CarKeys.FOB_RANGE).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.reikocars.car_key.rekey").withStyle(ChatFormatting.DARK_GRAY));
    }
}
