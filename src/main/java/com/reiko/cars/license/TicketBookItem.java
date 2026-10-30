package com.reiko.cars.license;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A police officer's Ticket Book: right-click a car to check its plate and its driver's license, or a player to check
 * theirs. Unlicensed drivers are ticketed (see {@link TicketService}). Only Police Officers (and admins) can use it.
 */
public final class TicketBookItem extends Item {
    public TicketBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer officer && target instanceof ServerPlayer checked) {
            TicketService.checkPlayer(officer, checked);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars.ticket_book.hint").withStyle(ChatFormatting.GRAY));
    }
}
