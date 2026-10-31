package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.fare.FareMenu;
import com.reiko.cars.fare.FareSettingsMenu;
import com.reiko.cars.mail.MailboxMenu;
import com.reiko.cars.pump.PumpMenu;
import com.reiko.cars.repair.RepairBenchMenu;
import com.reiko.cars.sale.CarSaleMenu;
import com.reiko.cars.parking.ParkingMeterMenu;
import com.reiko.cars.toll.TollBoothMenu;
import com.reiko.cars.toll.TollMenu;
import com.reiko.cars.sale.DealershipMenu;
import com.reiko.cars.workbench.CarWorkbenchMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ReikoCars.MODID);

    public static final Supplier<MenuType<PumpMenu>> PUMP = MENUS.register("gas_pump", () -> IMenuTypeExtension.create(PumpMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.charge.ChargerMenu>> CHARGER = MENUS.register("charging_station",
            () -> IMenuTypeExtension.create(com.reiko.cars.charge.ChargerMenu::new));
    public static final Supplier<MenuType<CarWorkbenchMenu>> CAR_WORKBENCH = MENUS.register("car_workbench",
            () -> new MenuType<>(CarWorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS));

    public static final Supplier<MenuType<RepairBenchMenu>> REPAIR_BENCH = MENUS.register("repair_bench",
            () -> IMenuTypeExtension.create(RepairBenchMenu::new));

    public static final Supplier<MenuType<MailboxMenu>> MAILBOX = MENUS.register("mailbox",
            () -> IMenuTypeExtension.create(MailboxMenu::new));

    public static final Supplier<MenuType<FareMenu>> FARE = MENUS.register("fare", () -> IMenuTypeExtension.create(FareMenu::new));
    public static final Supplier<MenuType<FareSettingsMenu>> FARE_SETTINGS = MENUS.register("fare_settings",
            () -> IMenuTypeExtension.create(FareSettingsMenu::new));

    public static final Supplier<MenuType<CarSaleMenu>> CAR_SALE = MENUS.register("car_sale", () -> IMenuTypeExtension.create(CarSaleMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.impound.ImpoundMenu>> IMPOUND = MENUS.register("impound",
            () -> IMenuTypeExtension.create(com.reiko.cars.impound.ImpoundMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.tuning.TuningBenchMenu>> TUNING_BENCH = MENUS.register("tuning_bench",
            () -> IMenuTypeExtension.create(com.reiko.cars.tuning.TuningBenchMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.racing.RaceControlMenu>> RACE_CONTROL = MENUS.register("race_control",
            () -> IMenuTypeExtension.create(com.reiko.cars.racing.RaceControlMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.dmv.DmvMenu>> DMV = MENUS.register("dmv",
            () -> IMenuTypeExtension.create(com.reiko.cars.dmv.DmvMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.rental.RentalMenu>> RENTAL = MENUS.register("rental",
            () -> IMenuTypeExtension.create(com.reiko.cars.rental.RentalMenu::new));
    public static final Supplier<MenuType<DealershipMenu>> DEALERSHIP = MENUS.register("dealership",
            () -> IMenuTypeExtension.create(DealershipMenu::new));

    public static final Supplier<MenuType<ParkingMeterMenu>> PARKING_METER = MENUS.register("parking_meter",
            () -> IMenuTypeExtension.create(ParkingMeterMenu::new));
    public static final Supplier<MenuType<TollMenu>> TOLL = MENUS.register("toll", () -> IMenuTypeExtension.create(TollMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.radio.RadioMenu>> RADIO = MENUS.register("radio",
            () -> IMenuTypeExtension.create(com.reiko.cars.radio.RadioMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.bus.BusStopMenu>> BUS_STOP = MENUS.register("bus_stop",
            () -> IMenuTypeExtension.create(com.reiko.cars.bus.BusStopMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.trailer.CamperMenu>> CAMPER = MENUS.register("camper",
            () -> IMenuTypeExtension.create(com.reiko.cars.trailer.CamperMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.road.PaverMenu>> PAVER = MENUS.register("road_paver",
            () -> IMenuTypeExtension.create(com.reiko.cars.road.PaverMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.traffic.RoadSignMenu>> ROAD_SIGN = MENUS.register("road_sign",
            () -> IMenuTypeExtension.create(com.reiko.cars.traffic.RoadSignMenu::new));
    public static final Supplier<MenuType<TollBoothMenu>> TOLL_BOOTH = MENUS.register("toll_booth", () -> IMenuTypeExtension.create(TollBoothMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.wash.WashPayMenu>> WASH_PAY = MENUS.register("wash_pay",
            () -> IMenuTypeExtension.create(com.reiko.cars.wash.WashPayMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.wash.WashOwnerMenu>> WASH_OWNER = MENUS.register("wash_owner",
            () -> IMenuTypeExtension.create(com.reiko.cars.wash.WashOwnerMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.icecream.IceCreamMenu>> ICE_CREAM = MENUS.register("ice_cream",
            () -> IMenuTypeExtension.create(com.reiko.cars.icecream.IceCreamMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.insurance.InsuranceMenu>> INSURANCE = MENUS.register("insurance",
            () -> IMenuTypeExtension.create(com.reiko.cars.insurance.InsuranceMenu::new));

    public static final Supplier<MenuType<com.reiko.cars.garage.GarageMenu>> GARAGE = MENUS.register("garage",
            () -> IMenuTypeExtension.create(com.reiko.cars.garage.GarageMenu::new));
    public static final Supplier<MenuType<com.reiko.cars.garage.ParkingMenu>> PARKING = MENUS.register("parking",
            () -> IMenuTypeExtension.create(com.reiko.cars.garage.ParkingMenu::new));

    private ModMenus() {
    }
}
