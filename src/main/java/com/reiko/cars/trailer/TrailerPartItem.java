package com.reiko.cars.trailer;

import com.reiko.cars.registry.ModItems;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * A trailer part, built into a trailer at the Car Workbench with four tires: the Trailer Chassis, which every trailer
 * takes, or one trailer's own body (the box, the tank or the car deck).
 */
public final class TrailerPartItem extends Item {
    /** The trailer this is the body of, or null for the chassis. */
    @Nullable
    private final TrailerType type;

    public TrailerPartItem(@Nullable TrailerType type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Nullable
    public TrailerType trailerType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add((type == null ? Component.translatable("item.reikocars.trailer_chassis.for")
                : Component.translatable("item.reikocars.part.for", Component.translatable(ModItems.trailer(type).getDescriptionId())))
                .withStyle(ChatFormatting.GRAY));
    }
}
