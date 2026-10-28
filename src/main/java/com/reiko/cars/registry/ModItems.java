package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.workbench.CarPart;
import com.reiko.cars.workbench.CarPartItem;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ReikoCars.MODID);

    private static final Map<CarType, DeferredItem<CarItem>> CARS = new EnumMap<>(CarType.class);
    private static final Map<CarType, Map<CarPart, DeferredItem<CarPartItem>>> PARTS = new EnumMap<>(CarType.class);

    static {
        for (CarType type : CarType.values()) {
            CARS.put(type, ITEMS.register(type.id(), () -> new CarItem(type, new Item.Properties().stacksTo(1))));
        }
        for (CarType type : CarType.values()) {
            Map<CarPart, DeferredItem<CarPartItem>> parts = new EnumMap<>(CarPart.class);
            for (CarPart part : CarPart.values()) {
                parts.put(part, ITEMS.register(type.id() + "_" + part.id(),
                        () -> new CarPartItem(type, part, new Item.Properties().stacksTo(16))));
            }
            PARTS.put(type, parts);
        }
    }

    public static final DeferredItem<Item> TIRE = ITEMS.registerSimpleItem("tire");
    public static final DeferredItem<Item> CAR_ENGINE = ITEMS.registerSimpleItem("car_engine");
    public static final DeferredItem<DoubleHighBlockItem> GAS_PUMP = ITEMS.register("gas_pump",
            () -> new DoubleHighBlockItem(ModBlocks.GAS_PUMP.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> DIESEL_PUMP = ITEMS.register("diesel_pump",
            () -> new DoubleHighBlockItem(ModBlocks.DIESEL_PUMP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CAR_WORKBENCH = ITEMS.registerSimpleBlockItem(ModBlocks.CAR_WORKBENCH);

    private ModItems() {
    }

    public static CarItem car(CarType type) {
        return CARS.get(type).get();
    }

    /** {@code type}'s chassis, body or interior, like {@code sedan_chassis}. */
    public static CarPartItem part(CarType type, CarPart part) {
        return PARTS.get(type).get(part).get();
    }
}
