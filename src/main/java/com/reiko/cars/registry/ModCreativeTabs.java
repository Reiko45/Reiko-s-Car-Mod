package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.trailer.TrailerType;
import com.reiko.cars.workbench.CarPart;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ReikoCars.MODID);

    public static final Supplier<CreativeModeTab> CARS = TABS.register("cars", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.reikocars"))
            .icon(() -> new ItemStack(ModItems.car(CarType.SEDAN)))
            .displayItems((parameters, output) -> {
                for (CarType type : CarType.values()) {
                    output.accept(ModItems.car(type));
                }
                // Full-tank cars, handy for testing in creative.
                for (CarType type : CarType.values()) {
                    output.accept(CarItem.withFuel(new ItemStack(ModItems.car(type)), type.tankCapacity()));
                }
                output.accept(ModItems.CAR_WORKBENCH.get());
                output.accept(ModItems.GAS_PUMP.get());
                output.accept(ModItems.DIESEL_PUMP.get());
                for (TrailerType type : TrailerType.values()) {
                    output.accept(ModItems.trailer(type));
                }
                output.accept(ModItems.CAR_ENGINE.get());
                output.accept(ModItems.DIESEL_ENGINE.get());
                output.accept(ModItems.TIRE.get());
                for (CarType type : CarType.values()) {
                    for (CarPart part : CarPart.values()) {
                        output.accept(ModItems.part(type, part));
                    }
                }
            })
            .build());

    private ModCreativeTabs() {
    }
}
