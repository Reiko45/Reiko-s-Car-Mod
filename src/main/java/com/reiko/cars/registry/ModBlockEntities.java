package com.reiko.cars.registry;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.mail.MailboxBlockEntity;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.sale.DealershipBlockEntity;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import com.reiko.cars.toll.TollBoothBlockEntity;
import com.reiko.cars.toll.TollGateBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ReikoCars.MODID);

    /** Gas and diesel pumps share it. Only the bottom half of a pump has one. */
    public static final Supplier<BlockEntityType<GasPumpBlockEntity>> GAS_PUMP = BLOCK_ENTITIES.register("gas_pump",
            () -> BlockEntityType.Builder.of(GasPumpBlockEntity::new, ModBlocks.GAS_PUMP.get(), ModBlocks.DIESEL_PUMP.get()).build(null));

    public static final Supplier<BlockEntityType<RepairBenchBlockEntity>> REPAIR_BENCH = BLOCK_ENTITIES.register("repair_bench",
            () -> BlockEntityType.Builder.of(RepairBenchBlockEntity::new, ModBlocks.REPAIR_BENCH.get()).build(null));

    public static final Supplier<BlockEntityType<MailboxBlockEntity>> MAILBOX = BLOCK_ENTITIES.register("mailbox",
            () -> BlockEntityType.Builder.of(MailboxBlockEntity::new, ModBlocks.MAILBOX.get()).build(null));

    public static final Supplier<BlockEntityType<DealershipBlockEntity>> DEALERSHIP = BLOCK_ENTITIES.register("dealership",
            () -> BlockEntityType.Builder.of(DealershipBlockEntity::new, ModBlocks.DEALERSHIP.get()).build(null));

    public static final Supplier<BlockEntityType<ParkingMeterBlockEntity>> PARKING_METER = BLOCK_ENTITIES.register("parking_meter",
            () -> BlockEntityType.Builder.of(ParkingMeterBlockEntity::new, ModBlocks.PARKING_METER.get()).build(null));
    public static final Supplier<BlockEntityType<TollBoothBlockEntity>> TOLL_BOOTH = BLOCK_ENTITIES.register("toll_booth",
            () -> BlockEntityType.Builder.of(TollBoothBlockEntity::new, ModBlocks.TOLL_BOOTH.get()).build(null));
    public static final Supplier<BlockEntityType<TollGateBlockEntity>> TOLL_GATE = BLOCK_ENTITIES.register("toll_gate",
            () -> BlockEntityType.Builder.of(TollGateBlockEntity::new, ModBlocks.TOLL_GATE.get()).build(null));

    private ModBlockEntities() {
    }
}
