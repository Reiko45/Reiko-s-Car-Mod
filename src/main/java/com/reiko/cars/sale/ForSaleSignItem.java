package com.reiko.cars.sale;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Right-click your car with it to put it up for sale at a price (or take it off sale). It isn't used up. */
public final class ForSaleSignItem extends Item {
    public ForSaleSignItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars.for_sale_sign.help").withStyle(ChatFormatting.GRAY));
    }
}
