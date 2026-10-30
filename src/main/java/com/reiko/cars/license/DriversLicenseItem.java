package com.reiko.cars.license;

import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.rental.RentalService;
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

/**
 * A driver's license card, handed out at a DMV. It shows whose it is, its number and its points; right-click to
 * check it against the records (it updates itself). The license itself is the record: losing the card loses
 * nothing but the card, and a DMV prints another.
 */
public final class DriversLicenseItem extends Item {
    public DriversLicenseItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        LicenseCard card = stack.get(ModDataComponents.LICENSE.get());
        if (player instanceof ServerPlayer serverPlayer && card != null) {
            DriverLicense license = LicenseService.of(serverPlayer.server, card.holder());
            if (license == null || !license.number().equals(card.number())) {
                serverPlayer.sendSystemMessage(Component.translatable("license.reikocars.card_void", card.name(), card.number()));
            } else {
                LicenseService.refreshCards(serverPlayer.server, license);
                serverPlayer.sendSystemMessage(Component.translatable("license.reikocars.card_check", card.name(),
                        LicenseService.statusLine(serverPlayer.server, card.holder())));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        LicenseCard card = stack.get(ModDataComponents.LICENSE.get());
        if (card == null) {
            tooltip.add(Component.translatable("item.reikocars.drivers_license.blank").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("item.reikocars.drivers_license.holder", card.name()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reikocars.drivers_license.number", card.number()).withStyle(ChatFormatting.GRAY));
        long now = context.level() == null ? 0 : context.level().getGameTime();
        if (now > 0 && now < card.suspendedUntil()) {
            tooltip.add(Component.translatable("item.reikocars.drivers_license.suspended", RentalService.timeLeft(card.suspendedUntil() - now))
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.reikocars.drivers_license.points", card.points(), LicenseService.maxPoints())
                    .withStyle(card.points() > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }
    }
}
