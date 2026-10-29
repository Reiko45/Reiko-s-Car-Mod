package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.mail.MailboxBlock;
import com.reiko.cars.pump.GasPumpBlock;
import com.reiko.cars.repair.RepairBenchBlock;
import com.reiko.cars.sale.DealershipBlock;
import com.reiko.cars.parking.ParkingMeterBlock;
import com.reiko.cars.toll.TollBoothBlock;
import com.reiko.cars.toll.TollGateBlock;
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
                    .sound(SoundType.METAL)
                    .noOcclusion())); // a bench on legs, open underneath: don't hide the faces behind it

    /** Where cars are serviced and repaired: by their drivers with their own parts, or by the bench's owner for money. */
    public static final DeferredBlock<RepairBenchBlock> REPAIR_BENCH = BLOCKS.register("repair_bench", () -> new RepairBenchBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    /** Where mail is sent from and comes to. Blast-proof, so nobody can blow up someone else's mail. */
    public static final DeferredBlock<MailboxBlock> MAILBOX = BLOCKS.register("mailbox", () -> new MailboxBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /** A car dealership's sales desk. Blast-proof, like the shops. */
    public static final DeferredBlock<DealershipBlock> DEALERSHIP = BLOCKS.register("dealership", () -> new DealershipBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /** A parking meter on a post. Blast-proof, like the shops. */
    public static final DeferredBlock<ParkingMeterBlock> PARKING_METER = BLOCKS.register("parking_meter", () -> new ParkingMeterBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.5F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /** A toll booth: the toll for its owner's gates nearby. */
    public static final DeferredBlock<com.reiko.cars.insurance.InsuranceOfficeBlock> INSURANCE_OFFICE = BLOCKS.register("insurance_office",
            () -> new com.reiko.cars.insurance.InsuranceOfficeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<TollBoothBlock> TOLL_BOOTH = BLOCKS.register("toll_booth", () -> new TollBoothBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /** A toll gate: a post and a barrier arm across three blocks of road. */
    public static final DeferredBlock<TollGateBlock> TOLL_GATE = BLOCKS.register("toll_gate", () -> new TollGateBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .dynamicShape()
                    .pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {
    }
}
