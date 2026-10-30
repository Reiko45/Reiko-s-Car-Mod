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
                // Full-tank cars, handy for testing in creative (the fire truck with a full water tank too).
                for (CarType type : CarType.values()) {
                    ItemStack full = CarItem.withFuel(new ItemStack(ModItems.car(type)), type.tankCapacity());
                    if (type.isFireTruck()) {
                        CarItem.withWater(full, com.reiko.cars.fire.FireTrucks.tankCapacity());
                    }
                    output.accept(full);
                }
                output.accept(ModItems.CAR_WORKBENCH.get());
                output.accept(ModItems.GAS_PUMP.get());
                output.accept(ModItems.DIESEL_PUMP.get());
                output.accept(ModItems.CHARGING_STATION.get());
                output.accept(ModItems.REPAIR_BENCH.get());
                output.accept(ModItems.TUNING_BENCH.get());
                for (com.reiko.cars.tuning.TuningKit kit : com.reiko.cars.tuning.TuningKit.values()) {
                    output.accept(ModItems.kit(kit));
                }
                output.accept(ModItems.MAILBOX.get());
                output.accept(ModItems.BLANK_KEY.get());
                output.accept(ModItems.DEALERSHIP.get());
                output.accept(ModItems.RENTAL_DESK.get());
                output.accept(ModItems.IMPOUND_OFFICE.get());
                output.accept(ModItems.DMV_OFFICE.get());
                output.accept(ModItems.TRAFFIC_CONE.get());
                output.accept(ModItems.TICKET_BOOK.get());
                output.accept(ModItems.RACE_CONTROL.get());
                output.accept(ModItems.RACE_FLAG.get());
                output.accept(ModItems.FOR_SALE_SIGN.get());
                output.accept(ModItems.PARKING_METER.get());
                output.accept(ModItems.TOLL_BOOTH.get());
                output.accept(ModItems.TOLL_GATE.get());
                output.accept(ModItems.INSURANCE_OFFICE.get());
                output.accept(ModItems.TRASH_CAN.get());
                output.accept(ModItems.FIRE_HYDRANT.get());
                output.accept(ModItems.FIRE_HOSE.get());
                output.accept(ModItems.ASPHALT.get());
                output.accept(ModItems.ASPHALT_SLAB.get());
                output.accept(ModItems.ASPHALT_STAIRS.get());
                output.accept(ModItems.ROAD_PAINT.get());
                output.accept(ModItems.STREET_POLE.get());
                output.accept(ModItems.TRAFFIC_LIGHT.get());
                output.accept(ModItems.STOP_SIGN.get());
                output.accept(ModItems.SPEED_SIGN.get());
                output.accept(ModItems.STREET_LAMP.get());
                output.accept(ModItems.VANILLA_ICE_CREAM.get());
                output.accept(ModItems.CHOCOLATE_ICE_CREAM.get());
                output.accept(ModItems.STRAWBERRY_ICE_CREAM.get());
                for (TrailerType type : TrailerType.values()) {
                    output.accept(ModItems.trailer(type));
                }
                output.accept(ModItems.CAR_ENGINE.get());
                output.accept(ModItems.DIESEL_ENGINE.get());
                output.accept(ModItems.MOTORCYCLE_ENGINE.get());
                output.accept(ModItems.ELECTRIC_MOTOR.get());
                output.accept(ModItems.BATTERY_PACK.get());
                output.accept(ModItems.TIRE.get());
                output.accept(ModItems.MOTOR_OIL.get());
                output.accept(ModItems.ENGINE_PARTS.get());
                output.accept(ModItems.BRAKE_PADS.get());
                output.accept(ModItems.BODY_PANEL.get());
                for (CarType type : CarType.values()) {
                    for (CarPart part : CarPart.values()) {
                        output.accept(ModItems.part(type, part));
                    }
                }
                output.accept(ModItems.TRAILER_CHASSIS.get());
                for (TrailerType type : TrailerType.values()) {
                    output.accept(ModItems.trailerBody(type));
                }
            })
            .build());

    private ModCreativeTabs() {
    }
}
