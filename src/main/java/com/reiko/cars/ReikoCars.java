package com.reiko.cars;

import com.mojang.logging.LogUtils;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.registry.ModCreativeTabs;
import com.reiko.cars.registry.ModDataComponents;
import com.reiko.cars.registry.ModEntities;
import com.reiko.cars.registry.ModItems;
import com.reiko.cars.registry.ModMenus;
import com.reiko.cars.registry.ModRecipes;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Reiko's Car Mod: drivable cars that burn BuildCraft Reborn's fuels, and gas pumps that sell fuel for cash or
 * card through Reiko's Economy Mod.
 */
@Mod(ReikoCars.MODID)
public final class ReikoCars {
    public static final String MODID = "reikocars";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ReikoCars(IEventBus modBus, ModContainer container) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModRecipes.TYPES.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, CarsConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
