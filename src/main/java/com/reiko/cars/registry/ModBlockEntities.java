package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.pump.GasPumpBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReikoCars.MODID);

    /** Only the bottom half of a pump has a block entity. */
    public static final Supplier<BlockEntityType<GasPumpBlockEntity>> GAS_PUMP = BLOCK_ENTITIES.register("gas_pump",
            () -> BlockEntityType.Builder.of(GasPumpBlockEntity::new, ModBlocks.GAS_PUMP.get()).build(null));

    private ModBlockEntities() {
    }
}
