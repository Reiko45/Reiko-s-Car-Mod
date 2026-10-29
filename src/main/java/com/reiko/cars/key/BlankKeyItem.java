package com.reiko.cars.key;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** An uncut key: sneak and right-click your car or trailer with it to cut a key for it. */
public final class BlankKeyItem extends Item {
    public BlankKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars.blank_key.help").withStyle(ChatFormatting.GRAY));
    }
}
