package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.pump.GasPumpBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ReikoCars.MODID);

    /** Two blocks tall. Blast-proof like the economy mod's shops, so nobody can blow up someone else's fuel. */
    public static final DeferredBlock<GasPumpBlock> GAS_PUMP = BLOCKS.register("gas_pump", () -> new GasPumpBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.5F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .lightLevel(state -> 5)));

    private ModBlocks() {
    }
}
