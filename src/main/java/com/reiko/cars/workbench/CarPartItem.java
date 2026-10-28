package com.reiko.cars.workbench;

import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A chassis, body or interior for one kind of car. */
public final class CarPartItem extends Item {
    private final CarType type;
    private final CarPart part;

    public CarPartItem(CarType type, CarPart part, Properties properties) {
        super(properties);
        this.type = type;
        this.part = part;
    }

    public CarType carType() {
        return type;
    }

    public CarPart part() {
        return part;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.reikocars.part.for", Component.translatable(ModItems.car(type).getDescriptionId()))
                .withStyle(ChatFormatting.GRAY));
    }
}
