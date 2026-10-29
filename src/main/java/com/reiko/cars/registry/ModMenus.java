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
    public static final Supplier<MenuType<DealershipMenu>> DEALERSHIP = MENUS.register("dealership",
            () -> IMenuTypeExtension.create(DealershipMenu::new));

    public static final Supplier<MenuType<ParkingMeterMenu>> PARKING_METER = MENUS.register("parking_meter",
            () -> IMenuTypeExtension.create(ParkingMeterMenu::new));
    public static final Supplier<MenuType<TollMenu>> TOLL = MENUS.register("toll", () -> IMenuTypeExtension.create(TollMenu::new));
    public static final Supplier<MenuType<TollBoothMenu>> TOLL_BOOTH = MENUS.register("toll_booth", () -> IMenuTypeExtension.create(TollBoothMenu::new));

    private ModMenus() {
    }
}
