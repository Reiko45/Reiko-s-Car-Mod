package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.pump.PumpMenu;
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

    private ModMenus() {
    }
}
