package com.reiko.cars.mail;

import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModItems;
import com.reiko.economy.api.EconomyApi;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A parcel: items wrapped up for someone, made at a mailbox. Its addressee opens it with a right-click; so can its
 * sender, if it never got there (return to sender). Nobody else can, though anyone can carry it.
 */
public final class ParcelItem extends Item {
    public ParcelItem(Properties properties) {
        super(properties);
    }

    /** A parcel item holding {@code parcel}. */
    public static ItemStack of(Parcel parcel) {
        ItemStack stack = new ItemStack(ModItems.PARCEL.get());
        stack.set(ModDataComponents.PARCEL.get(), parcel);
        return stack;
    }

    @Nullable
    public static Parcel parcel(ItemStack stack) {
        return stack.get(ModDataComponents.PARCEL.get());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Parcel parcel = parcel(stack);
        if (parcel == null) {
            return InteractionResultHolder.pass(stack);
        }
        boolean mine = player.getUUID().equals(parcel.to()) || player.getUUID().equals(parcel.from());
        if (!level.isClientSide) {
            if (!mine) {
                player.displayClientMessage(Component.translatable("mail.reikocars.not_yours", parcel.toName()), true);
            } else {
                for (ItemStack inside : parcel.contents().nonEmptyItemsCopy()) {
                    player.getInventory().placeItemBackInInventory(inside);
                }
                player.displayClientMessage(Component.translatable(player.getUUID().equals(parcel.to())
                        ? "mail.reikocars.opened" : "mail.reikocars.opened_returned", parcel.fromName(), parcel.toName()), true);
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
                stack.shrink(1); // even in creative: the box is opened
            }
        }
        return mine ? InteractionResultHolder.sidedSuccess(stack, level.isClientSide) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Parcel parcel = parcel(stack);
        if (parcel == null) {
            return;
        }
        tooltip.add(Component.translatable("item.reikocars.parcel.to", parcel.toName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reikocars.parcel.from", parcel.fromName()).withStyle(ChatFormatting.GRAY));
        parcel.address().ifPresent(address -> tooltip.add(addressLine(address).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.translatable("item.reikocars.parcel.inside", parcel.stacks()).withStyle(ChatFormatting.DARK_GRAY));
        if (parcel.postage() > 0) {
            tooltip.add(Component.translatable("item.reikocars.parcel.postage", EconomyApi.get().format(parcel.postage()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static net.minecraft.network.chat.MutableComponent addressLine(GlobalPos address) {
        var pos = address.pos();
        String dimension = address.dimension().location().getPath().replace('_', ' ');
        return Component.translatable("item.reikocars.parcel.address", pos.getX(), pos.getY(), pos.getZ(), dimension);
    }
}
