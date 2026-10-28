package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ReikoCars.MODID);

    private static final Map<CarType, DeferredItem<CarItem>> CARS = new EnumMap<>(CarType.class);

    static {
        for (CarType type : CarType.values()) {
            CARS.put(type, ITEMS.register(type.id(), () -> new CarItem(type, new Item.Properties().stacksTo(1))));
        }
    }

    public static final DeferredItem<Item> TIRE = ITEMS.registerSimpleItem("tire");
    public static final DeferredItem<Item> CAR_ENGINE = ITEMS.registerSimpleItem("car_engine");
    public static final DeferredItem<DoubleHighBlockItem> GAS_PUMP = ITEMS.register("gas_pump",
            () -> new DoubleHighBlockItem(ModBlocks.GAS_PUMP.get(), new Item.Properties()));

    private ModItems() {
    }

    public static CarItem car(CarType type) {
        return CARS.get(type).get();
    }
}
