package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarItem;
import com.reiko.cars.car.CarType;
import com.reiko.cars.key.BlankKeyItem;
import com.reiko.cars.key.CarKeyItem;
import com.reiko.cars.mail.ParcelItem;
import com.reiko.cars.sale.ForSaleSignItem;
import com.reiko.cars.trailer.TrailerItem;
import com.reiko.cars.trailer.TrailerPartItem;
import com.reiko.cars.trailer.TrailerType;
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
    private static final Map<TrailerType, DeferredItem<TrailerItem>> TRAILERS = new EnumMap<>(TrailerType.class);
    private static final Map<TrailerType, DeferredItem<TrailerPartItem>> TRAILER_BODIES = new EnumMap<>(TrailerType.class);

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
        for (TrailerType type : TrailerType.values()) {
            TRAILERS.put(type, ITEMS.register(type.id(), () -> new TrailerItem(type, new Item.Properties().stacksTo(1))));
        }
        for (TrailerType type : TrailerType.values()) {
            TRAILER_BODIES.put(type, ITEMS.register(type.id() + "_body", () -> new TrailerPartItem(type, new Item.Properties().stacksTo(16))));
        }
    }

    /** Every trailer's chassis: built into a trailer at the workbench with that trailer's body and four tires. */
    public static final DeferredItem<TrailerPartItem> TRAILER_CHASSIS = ITEMS.register("trailer_chassis",
            () -> new TrailerPartItem(null, new Item.Properties().stacksTo(16)));

    public static final DeferredItem<Item> TIRE = ITEMS.registerSimpleItem("tire");
    public static final DeferredItem<Item> CAR_ENGINE = ITEMS.registerSimpleItem("car_engine");
    /** The semi's engine. */
    public static final DeferredItem<Item> DIESEL_ENGINE = ITEMS.registerSimpleItem("diesel_engine");
    /** Every motorcycle's engine. */
    public static final DeferredItem<Item> MOTORCYCLE_ENGINE = ITEMS.registerSimpleItem("motorcycle_engine");
    public static final DeferredItem<DoubleHighBlockItem> GAS_PUMP = ITEMS.register("gas_pump",
            () -> new DoubleHighBlockItem(ModBlocks.GAS_PUMP.get(), new Item.Properties()));
    public static final DeferredItem<DoubleHighBlockItem> DIESEL_PUMP = ITEMS.register("diesel_pump",
            () -> new DoubleHighBlockItem(ModBlocks.DIESEL_PUMP.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CAR_WORKBENCH = ITEMS.registerSimpleBlockItem(ModBlocks.CAR_WORKBENCH);
    public static final DeferredItem<BlockItem> REPAIR_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.REPAIR_BENCH);

    public static final DeferredItem<BlockItem> MAILBOX = ITEMS.registerSimpleBlockItem(ModBlocks.MAILBOX);
    /** Items wrapped up for someone at a mailbox. Only made there. */
    public static final DeferredItem<ParcelItem> PARCEL = ITEMS.register("parcel", () -> new ParcelItem(new Item.Properties().stacksTo(1)));

    /** An uncut key, cut for a car or trailer by its owner. */
    public static final DeferredItem<BlankKeyItem> BLANK_KEY = ITEMS.register("blank_key", () -> new BlankKeyItem(new Item.Properties()));
    /** A key cut for one car or trailer. Only made by cutting a blank. */
    public static final DeferredItem<CarKeyItem> CAR_KEY = ITEMS.register("car_key", () -> new CarKeyItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> DEALERSHIP = ITEMS.registerSimpleBlockItem(ModBlocks.DEALERSHIP);
    /** Puts a car up for sale anywhere. A tool: it isn't used up. */
    public static final DeferredItem<ForSaleSignItem> FOR_SALE_SIGN = ITEMS.register("for_sale_sign",
            () -> new ForSaleSignItem(new Item.Properties().stacksTo(16)));

    // Repair parts, fitted at a repair bench (see RepairJobs). Tires are the same ones cars are built with.
    /** An oil change. */
    public static final DeferredItem<Item> MOTOR_OIL = ITEMS.registerSimpleItem("motor_oil", new Item.Properties().stacksTo(16));
    /** A quarter of an engine rebuild. */
    public static final DeferredItem<Item> ENGINE_PARTS = ITEMS.registerSimpleItem("engine_parts");
    /** A set of brakes. */
    public static final DeferredItem<Item> BRAKE_PADS = ITEMS.registerSimpleItem("brake_pads", new Item.Properties().stacksTo(16));
    /** A fifth of a body repair. */
    public static final DeferredItem<Item> BODY_PANEL = ITEMS.registerSimpleItem("body_panel");

    private ModItems() {
    }

    public static CarItem car(CarType type) {
        return CARS.get(type).get();
    }

    public static TrailerItem trailer(TrailerType type) {
        return TRAILERS.get(type).get();
    }

    /** {@code type}'s body: the box, the tank or the car deck, like {@code box_trailer_body}. */
    public static TrailerPartItem trailerBody(TrailerType type) {
        return TRAILER_BODIES.get(type).get();
    }

    /** {@code type}'s chassis, body or interior, like {@code sedan_chassis}. */
    public static CarPartItem part(CarType type, CarPart part) {
        return PARTS.get(type).get(part).get();
    }
}
