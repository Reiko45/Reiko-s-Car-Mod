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
    public static final Supplier<BlockEntityType<com.reiko.cars.charge.ChargingStationBlockEntity>> CHARGING_STATION =
            BLOCK_ENTITIES.register("charging_station", () -> BlockEntityType.Builder.of(com.reiko.cars.charge.ChargingStationBlockEntity::new,
                    ModBlocks.CHARGING_STATION.get()).build(null));
    public static final Supplier<BlockEntityType<GasPumpBlockEntity>> GAS_PUMP = BLOCK_ENTITIES.register("gas_pump",
            () -> BlockEntityType.Builder.of(GasPumpBlockEntity::new, ModBlocks.GAS_PUMP.get(), ModBlocks.DIESEL_PUMP.get()).build(null));

    public static final Supplier<BlockEntityType<RepairBenchBlockEntity>> REPAIR_BENCH = BLOCK_ENTITIES.register("repair_bench",
            () -> BlockEntityType.Builder.of(RepairBenchBlockEntity::new, ModBlocks.REPAIR_BENCH.get()).build(null));

    public static final Supplier<BlockEntityType<MailboxBlockEntity>> MAILBOX = BLOCK_ENTITIES.register("mailbox",
            () -> BlockEntityType.Builder.of(MailboxBlockEntity::new, ModBlocks.MAILBOX.get()).build(null));

    public static final Supplier<BlockEntityType<com.reiko.cars.impound.ImpoundOfficeBlockEntity>> IMPOUND_OFFICE = BLOCK_ENTITIES.register(
            "impound_office", () -> BlockEntityType.Builder.of(com.reiko.cars.impound.ImpoundOfficeBlockEntity::new, ModBlocks.IMPOUND_OFFICE.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.tuning.TuningBenchBlockEntity>> TUNING_BENCH = BLOCK_ENTITIES.register(
            "tuning_bench", () -> BlockEntityType.Builder.of(com.reiko.cars.tuning.TuningBenchBlockEntity::new, ModBlocks.TUNING_BENCH.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.racing.RaceControlBlockEntity>> RACE_CONTROL = BLOCK_ENTITIES.register(
            "race_control", () -> BlockEntityType.Builder.of(com.reiko.cars.racing.RaceControlBlockEntity::new, ModBlocks.RACE_CONTROL.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.racing.RaceFlagBlockEntity>> RACE_FLAG = BLOCK_ENTITIES.register(
            "race_flag", () -> BlockEntityType.Builder.of(com.reiko.cars.racing.RaceFlagBlockEntity::new, ModBlocks.RACE_FLAG.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.dmv.DmvOfficeBlockEntity>> DMV_OFFICE = BLOCK_ENTITIES.register(
            "dmv_office", () -> BlockEntityType.Builder.of(com.reiko.cars.dmv.DmvOfficeBlockEntity::new, ModBlocks.DMV_OFFICE.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.road.TrafficConeBlockEntity>> TRAFFIC_CONE = BLOCK_ENTITIES.register(
            "traffic_cone", () -> BlockEntityType.Builder.of(com.reiko.cars.road.TrafficConeBlockEntity::new, ModBlocks.TRAFFIC_CONE.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.rental.RentalDeskBlockEntity>> RENTAL_DESK = BLOCK_ENTITIES.register(
            "rental_desk", () -> BlockEntityType.Builder.of(com.reiko.cars.rental.RentalDeskBlockEntity::new, ModBlocks.RENTAL_DESK.get()).build(null));
    public static final Supplier<BlockEntityType<DealershipBlockEntity>> DEALERSHIP = BLOCK_ENTITIES.register("dealership",
            () -> BlockEntityType.Builder.of(DealershipBlockEntity::new, ModBlocks.DEALERSHIP.get()).build(null));

    public static final Supplier<BlockEntityType<ParkingMeterBlockEntity>> PARKING_METER = BLOCK_ENTITIES.register("parking_meter",
            () -> BlockEntityType.Builder.of(ParkingMeterBlockEntity::new, ModBlocks.PARKING_METER.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.insurance.InsuranceOfficeBlockEntity>> INSURANCE_OFFICE = BLOCK_ENTITIES.register(
            "insurance_office", () -> BlockEntityType.Builder.of(com.reiko.cars.insurance.InsuranceOfficeBlockEntity::new,
                    ModBlocks.INSURANCE_OFFICE.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.garbage.TrashCanBlockEntity>> TRASH_CAN = BLOCK_ENTITIES.register(
            "trash_can", () -> BlockEntityType.Builder.of(com.reiko.cars.garbage.TrashCanBlockEntity::new, ModBlocks.TRASH_CAN.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.fire.FireHydrantBlockEntity>> FIRE_HYDRANT = BLOCK_ENTITIES.register(
            "fire_hydrant", () -> BlockEntityType.Builder.of(com.reiko.cars.fire.FireHydrantBlockEntity::new, ModBlocks.FIRE_HYDRANT.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.traffic.TrafficLightBlockEntity>> TRAFFIC_LIGHT = BLOCK_ENTITIES.register("traffic_light",
            () -> BlockEntityType.Builder.of(com.reiko.cars.traffic.TrafficLightBlockEntity::new, ModBlocks.TRAFFIC_LIGHT.get()).build(null));
    public static final Supplier<BlockEntityType<com.reiko.cars.traffic.SpeedSignBlockEntity>> SPEED_SIGN = BLOCK_ENTITIES.register("speed_sign",
            () -> BlockEntityType.Builder.of(com.reiko.cars.traffic.SpeedSignBlockEntity::new, ModBlocks.SPEED_SIGN.get()).build(null));
    public static final Supplier<BlockEntityType<TollBoothBlockEntity>> TOLL_BOOTH = BLOCK_ENTITIES.register("toll_booth",
            () -> BlockEntityType.Builder.of(TollBoothBlockEntity::new, ModBlocks.TOLL_BOOTH.get()).build(null));
    public static final Supplier<BlockEntityType<TollGateBlockEntity>> TOLL_GATE = BLOCK_ENTITIES.register("toll_gate",
            () -> BlockEntityType.Builder.of(TollGateBlockEntity::new, ModBlocks.TOLL_GATE.get()).build(null));

    private ModBlockEntities() {
    }
}
