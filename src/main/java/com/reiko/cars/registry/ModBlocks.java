package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.workbench.CarWorkbenchBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ReikoCars.MODID);

    /** Two blocks tall. Blast-proof like the economy mod's shops, so nobody can blow up someone else's fuel. */
    public static final DeferredBlock<GasPumpBlock> GAS_PUMP = BLOCKS.register("gas_pump",
            () -> new GasPumpBlock(FuelType.GASOLINE, pump(MapColor.COLOR_RED)));
    /** The same pump, selling diesel. */
    public static final DeferredBlock<GasPumpBlock> DIESEL_PUMP = BLOCKS.register("diesel_pump",
            () -> new GasPumpBlock(FuelType.DIESEL, pump(MapColor.COLOR_GREEN)));

    private static BlockBehaviour.Properties pump(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(3.5F, 1200.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL)
                .noOcclusion()
                .pushReaction(PushReaction.BLOCK)
                .lightLevel(state -> 5);
    }

    /** Where cars are put together from their parts. */
    public static final DeferredBlock<CarWorkbenchBlock> CAR_WORKBENCH = BLOCKS.register("car_workbench", () -> new CarWorkbenchBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    private ModBlocks() {
    }
}
