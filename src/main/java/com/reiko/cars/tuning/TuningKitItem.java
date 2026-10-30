package com.reiko.cars.tuning;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A tuning kit: fitted to a car at a Tuning Bench. */
public final class TuningKitItem extends Item {
    private final TuningKit kit;

    public TuningKitItem(TuningKit kit, Properties properties) {
        super(properties);
        this.kit = kit;
    }

    public TuningKit kit() {
        return kit;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars." + kit.id() + ".effect").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.reikocars." + kit.id() + ".cost").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.reikocars.tuning_kit.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
