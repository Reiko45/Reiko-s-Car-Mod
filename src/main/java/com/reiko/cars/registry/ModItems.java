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
    private static final Map<com.reiko.cars.tuning.TuningKit, DeferredItem<com.reiko.cars.tuning.TuningKitItem>> KITS =
            new EnumMap<>(com.reiko.cars.tuning.TuningKit.class);

    static {
        for (com.reiko.cars.tuning.TuningKit kit : com.reiko.cars.tuning.TuningKit.values()) {
            KITS.put(kit, ITEMS.register(kit.id(), () -> new com.reiko.cars.tuning.TuningKitItem(kit, new Item.Properties().stacksTo(16))));
        }
    }

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
    /** Every electric vehicle's engine. */
    public static final DeferredItem<Item> ELECTRIC_MOTOR = ITEMS.registerSimpleItem("electric_motor");
    /** An electric vehicle's battery, built into its chassis; the charging station takes one too. */
    public static final DeferredItem<Item> BATTERY_PACK = ITEMS.registerSimpleItem("battery_pack");
    public static final DeferredItem<DoubleHighBlockItem> CHARGING_STATION = ITEMS.register("charging_station",
            () -> new DoubleHighBlockItem(ModBlocks.CHARGING_STATION.get(), new Item.Properties()));
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
    public static final DeferredItem<BlockItem> IMPOUND_OFFICE = ITEMS.registerSimpleBlockItem(ModBlocks.IMPOUND_OFFICE);
    public static final DeferredItem<BlockItem> RENTAL_DESK = ITEMS.registerSimpleBlockItem(ModBlocks.RENTAL_DESK);
    public static final DeferredItem<BlockItem> DMV_OFFICE = ITEMS.registerSimpleBlockItem(ModBlocks.DMV_OFFICE);
    public static final DeferredItem<BlockItem> TUNING_BENCH = ITEMS.registerSimpleBlockItem(ModBlocks.TUNING_BENCH);
    public static final DeferredItem<BlockItem> RACE_CONTROL = ITEMS.registerSimpleBlockItem(ModBlocks.RACE_CONTROL);
    public static final DeferredItem<BlockItem> RACE_FLAG = ITEMS.registerSimpleBlockItem(ModBlocks.RACE_FLAG);
    public static final DeferredItem<BlockItem> TRAFFIC_CONE = ITEMS.registerSimpleBlockItem(ModBlocks.TRAFFIC_CONE);
    /** A driver's license card, from passing the test at a DMV. */
    public static final DeferredItem<com.reiko.cars.license.DriversLicenseItem> DRIVERS_LICENSE = ITEMS.register("drivers_license",
            () -> new com.reiko.cars.license.DriversLicenseItem(new Item.Properties().stacksTo(1)));
    /** A police officer's Ticket Book: plate and license checks. */
    public static final DeferredItem<com.reiko.cars.license.TicketBookItem> TICKET_BOOK = ITEMS.register("ticket_book",
            () -> new com.reiko.cars.license.TicketBookItem(new Item.Properties().stacksTo(1)));
    /** Puts a car up for sale anywhere. A tool: it isn't used up. */
    public static final DeferredItem<ForSaleSignItem> FOR_SALE_SIGN = ITEMS.register("for_sale_sign",
            () -> new ForSaleSignItem(new Item.Properties().stacksTo(16)));

    public static final DeferredItem<BlockItem> PARKING_METER = ITEMS.registerSimpleBlockItem(ModBlocks.PARKING_METER);
    public static final DeferredItem<BlockItem> TOLL_BOOTH = ITEMS.registerSimpleBlockItem(ModBlocks.TOLL_BOOTH);
    public static final DeferredItem<BlockItem> INSURANCE_OFFICE = ITEMS.registerSimpleBlockItem(ModBlocks.INSURANCE_OFFICE);
    public static final DeferredItem<BlockItem> TRASH_CAN = ITEMS.registerSimpleBlockItem(ModBlocks.TRASH_CAN);
    public static final DeferredItem<BlockItem> FIRE_HYDRANT = ITEMS.registerSimpleBlockItem(ModBlocks.FIRE_HYDRANT);
    public static final DeferredItem<com.reiko.cars.fire.FireHoseItem> FIRE_HOSE = ITEMS.register("fire_hose",
            () -> new com.reiko.cars.fire.FireHoseItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<BlockItem> TOLL_GATE = ITEMS.registerSimpleBlockItem(ModBlocks.TOLL_GATE);
    public static final DeferredItem<BlockItem> CAR_WASH = ITEMS.registerSimpleBlockItem(ModBlocks.CAR_WASH);
    public static final DeferredItem<BlockItem> WASH_BAY = ITEMS.registerSimpleBlockItem(ModBlocks.WASH_BAY);
    /** A Wash Bay's lance: the bay hands it out. */
    public static final DeferredItem<com.reiko.cars.wash.PressureWasherItem> PRESSURE_WASHER = ITEMS.register("pressure_washer",
            () -> new com.reiko.cars.wash.PressureWasherItem(new Item.Properties().stacksTo(1)));

    // Repair parts, fitted at a repair bench (see RepairJobs). Tires are the same ones cars are built with.
    /** An oil change. */
    public static final DeferredItem<Item> MOTOR_OIL = ITEMS.registerSimpleItem("motor_oil", new Item.Properties().stacksTo(16));
    /** A quarter of an engine rebuild. */
    public static final DeferredItem<Item> ENGINE_PARTS = ITEMS.registerSimpleItem("engine_parts");
    /** A set of brakes. */
    public static final DeferredItem<Item> BRAKE_PADS = ITEMS.registerSimpleItem("brake_pads", new Item.Properties().stacksTo(16));
    /** A fifth of a body repair. */
    public static final DeferredItem<Item> BODY_PANEL = ITEMS.registerSimpleItem("body_panel");
    /** A new window for a car: the windshield, the back window or a side window, cracked or shattered. */
    public static final DeferredItem<Item> CAR_WINDOW = ITEMS.registerSimpleItem("car_window", new Item.Properties().stacksTo(16));
    /** A new lamp cluster: a headlight or a taillight, with its turn signal. */
    public static final DeferredItem<Item> CAR_LIGHT = ITEMS.registerSimpleItem("car_light", new Item.Properties().stacksTo(16));

    public static final DeferredItem<BlockItem> GARAGE_DOOR = ITEMS.registerSimpleBlockItem(ModBlocks.GARAGE_DOOR);
    public static final DeferredItem<BlockItem> ROLLER_SHUTTER = ITEMS.registerSimpleBlockItem(ModBlocks.ROLLER_SHUTTER);
    public static final DeferredItem<BlockItem> GARAGE_BUTTON = ITEMS.registerSimpleBlockItem(ModBlocks.GARAGE_BUTTON);
    /** Opens and shuts the garage doors it's linked to. */
    public static final DeferredItem<com.reiko.cars.garage.GarageRemoteItem> GARAGE_REMOTE = ITEMS.register("garage_remote",
            () -> new com.reiko.cars.garage.GarageRemoteItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<BlockItem> ASPHALT = ITEMS.registerSimpleBlockItem(ModBlocks.ASPHALT);
    public static final DeferredItem<BlockItem> ASPHALT_SLAB = ITEMS.registerSimpleBlockItem(ModBlocks.ASPHALT_SLAB);
    public static final DeferredItem<BlockItem> ASPHALT_STAIRS = ITEMS.registerSimpleBlockItem(ModBlocks.ASPHALT_STAIRS);
    public static final DeferredItem<BlockItem> STREET_POLE = ITEMS.registerSimpleBlockItem(ModBlocks.STREET_POLE);
    public static final DeferredItem<BlockItem> TRAFFIC_LIGHT = ITEMS.registerSimpleBlockItem(ModBlocks.TRAFFIC_LIGHT);
    public static final DeferredItem<BlockItem> STOP_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.STOP_SIGN);
    public static final DeferredItem<BlockItem> BUS_STOP_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.BUS_STOP_SIGN);
    public static final DeferredItem<BlockItem> BUS_SHELTER = ITEMS.registerSimpleBlockItem(ModBlocks.BUS_SHELTER);
    public static final DeferredItem<BlockItem> SPEED_SIGN = ITEMS.registerSimpleBlockItem(ModBlocks.SPEED_SIGN);
    public static final DeferredItem<BlockItem> STREET_LAMP = ITEMS.registerSimpleBlockItem(ModBlocks.STREET_LAMP);
    /** Paints lines, crosswalks and arrows on asphalt. */
    public static final DeferredItem<com.reiko.cars.road.RoadPaintItem> ROAD_PAINT = ITEMS.register("road_paint",
            () -> new com.reiko.cars.road.RoadPaintItem(new Item.Properties().durability(256)));

    // Ice cream, for the ice cream truck's freezer: a quick snack.
    private static final net.minecraft.world.food.FoodProperties ICE_CREAM_FOOD = new net.minecraft.world.food.FoodProperties.Builder()
            .nutrition(3).saturationModifier(0.3F).fast().alwaysEdible().build();
    public static final DeferredItem<Item> VANILLA_ICE_CREAM = ITEMS.registerSimpleItem("vanilla_ice_cream", new Item.Properties().food(ICE_CREAM_FOOD));
    public static final DeferredItem<Item> CHOCOLATE_ICE_CREAM = ITEMS.registerSimpleItem("chocolate_ice_cream", new Item.Properties().food(ICE_CREAM_FOOD));
    public static final DeferredItem<Item> STRAWBERRY_ICE_CREAM = ITEMS.registerSimpleItem("strawberry_ice_cream", new Item.Properties().food(ICE_CREAM_FOOD));

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
    public static com.reiko.cars.tuning.TuningKitItem kit(com.reiko.cars.tuning.TuningKit kit) {
        return KITS.get(kit).get();
    }

    public static CarPartItem part(CarType type, CarPart part) {
        return PARTS.get(type).get(part).get();
    }
}
