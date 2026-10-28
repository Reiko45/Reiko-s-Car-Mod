package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarTank;
import com.reiko.cars.car.CarType;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.pump.GasPumpBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Pipes (BuildCraft's or any other mod's) can push fuel into either half of a gas pump, from any side. Cars expose
 * their tank too, for mods that fill entities.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class ModCapabilities {
    private ModCapabilities() {
    }

    @SubscribeEvent
    static void register(RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            GasPumpBlockEntity pump = GasPumpBlock.pumpAt(level, pos, state);
            return pump == null ? null : pump.pipeInput();
        }, ModBlocks.GAS_PUMP.get(), ModBlocks.DIESEL_PUMP.get());

        for (CarType type : CarType.values()) {
            event.registerEntity(Capabilities.FluidHandler.ENTITY, ModEntities.car(type), (car, context) -> new CarTank(car));
        }
    }
}
