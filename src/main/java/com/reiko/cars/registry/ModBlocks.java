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

    /** Sells electric cars their charge: power from BuildCraft engines and pipes, sold like fuel. Two blocks tall. */
    public static final DeferredBlock<com.reiko.cars.charge.ChargingStationBlock> CHARGING_STATION = BLOCKS.register("charging_station",
            () -> new com.reiko.cars.charge.ChargingStationBlock(pump(MapColor.COLOR_LIGHT_BLUE)));

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
    public static final DeferredBlock<com.reiko.cars.impound.ImpoundOfficeBlock> IMPOUND_OFFICE = BLOCKS.register("impound_office",
            () -> new com.reiko.cars.impound.ImpoundOfficeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    /** A tuning shop's bench. Blast-proof, like the Repair Bench. */
    public static final DeferredBlock<com.reiko.cars.tuning.TuningBenchBlock> TUNING_BENCH = BLOCKS.register("tuning_bench",
            () -> new com.reiko.cars.tuning.TuningBenchBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    /** A race track's Race Control. Blast-proof, like the shops. */
    public static final DeferredBlock<com.reiko.cars.racing.RaceControlBlock> RACE_CONTROL = BLOCKS.register("race_control",
            () -> new com.reiko.cars.racing.RaceControlBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    /** A race flag, which cars go through; numbered, a mark on a race track. */
    public static final DeferredBlock<com.reiko.cars.racing.RaceFlagBlock> RACE_FLAG = BLOCKS.register("race_flag",
            () -> new com.reiko.cars.racing.RaceFlagBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.SNOW)
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
    /** A DMV's counter: driving tests, license cards and custom plates. Blast-proof, like the shops. */
    public static final DeferredBlock<com.reiko.cars.dmv.DmvOfficeBlock> DMV_OFFICE = BLOCKS.register("dmv_office",
            () -> new com.reiko.cars.dmv.DmvOfficeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    /** A traffic cone, which cars go through; numbered, a mark on a DMV's test route. */
    public static final DeferredBlock<com.reiko.cars.road.TrafficConeBlock> TRAFFIC_CONE = BLOCKS.register("traffic_cone",
            () -> new com.reiko.cars.road.TrafficConeBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(0.5F, 6.0F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<com.reiko.cars.rental.RentalDeskBlock> RENTAL_DESK = BLOCKS.register("rental_desk",
            () -> new com.reiko.cars.rental.RentalDeskBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
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
    public static final DeferredBlock<com.reiko.cars.garbage.TrashCanBlock> TRASH_CAN = BLOCKS.register("trash_can",
            () -> new com.reiko.cars.garbage.TrashCanBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));
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

    /** The drive-through Car Wash: an arch of posts and a beam over four blocks of road. */
    public static final DeferredBlock<com.reiko.cars.wash.CarWashBlock> CAR_WASH = BLOCKS.register("car_wash", () -> new com.reiko.cars.wash.CarWashBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));
    /** A self-service Wash Bay: a coin-op pressure washer. */
    public static final DeferredBlock<com.reiko.cars.wash.WashBayBlock> WASH_BAY = BLOCKS.register("wash_bay", () -> new com.reiko.cars.wash.WashBayBlock(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    // ---- roads ----

    private static BlockBehaviour.Properties asphalt() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .instrument(net.minecraft.world.level.block.state.properties.NoteBlockInstrument.BASEDRUM)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE);
    }

    public static final DeferredBlock<com.reiko.cars.road.AsphaltBlock> ASPHALT = BLOCKS.register("asphalt",
            () -> new com.reiko.cars.road.AsphaltBlock(asphalt()));
    public static final DeferredBlock<com.reiko.cars.road.AsphaltSlabBlock> ASPHALT_SLAB = BLOCKS.register("asphalt_slab",
            () -> new com.reiko.cars.road.AsphaltSlabBlock(asphalt()));
    public static final DeferredBlock<net.minecraft.world.level.block.StairBlock> ASPHALT_STAIRS = BLOCKS.register("asphalt_stairs",
            () -> new net.minecraft.world.level.block.StairBlock(ASPHALT.get().defaultBlockState(), asphalt()));

    private static BlockBehaviour.Properties streetMetal() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static final DeferredBlock<com.reiko.cars.road.StreetPoleBlock> STREET_POLE = BLOCKS.register("street_pole",
            () -> new com.reiko.cars.road.StreetPoleBlock(streetMetal()));
    public static final DeferredBlock<com.reiko.cars.road.StopSignBlock> STOP_SIGN = BLOCKS.register("stop_sign",
            () -> new com.reiko.cars.road.StopSignBlock(streetMetal()));
    /** A BUS STOP sign on a pole. */
    public static final DeferredBlock<com.reiko.cars.bus.BusStopSignBlock> BUS_STOP_SIGN = BLOCKS.register("bus_stop_sign",
            () -> new com.reiko.cars.bus.BusStopSignBlock(streetMetal().pushReaction(PushReaction.BLOCK)));
    /** A bus shelter: glass, a roof and a bench, three blocks wide and two high. */
    public static final DeferredBlock<com.reiko.cars.bus.BusShelterBlock> BUS_SHELTER = BLOCKS.register("bus_shelter",
            () -> new com.reiko.cars.bus.BusShelterBlock(streetMetal().strength(2.5F, 1200.0F).pushReaction(PushReaction.BLOCK)));
    /** The light a car's headlights cast in the dark: invisible, moved along by the car, gone when it goes. */
    public static final DeferredBlock<com.reiko.cars.car.HeadlightGlowBlock> HEADLIGHT_GLOW = BLOCKS.register("headlight_glow",
            () -> new com.reiko.cars.car.HeadlightGlowBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .strength(-1.0F, 3600000.8F)
                    .noLootTable()
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(com.reiko.cars.car.HeadlightGlowBlock.LEVEL))));

    /** Where a fire truck's raised ladder stands: invisible and climbable, put there and taken away by the truck. */
    public static final DeferredBlock<com.reiko.cars.fire.FireLadderBlock> FIRE_LADDER = BLOCKS.register("fire_ladder",
            () -> new com.reiko.cars.fire.FireLadderBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .strength(-1.0F, 3600000.8F)
                    .noLootTable()
                    .noOcclusion()
                    .noCollission()
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<com.reiko.cars.fire.FireHydrantBlock> FIRE_HYDRANT = BLOCKS.register("fire_hydrant",
            () -> new com.reiko.cars.fire.FireHydrantBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredBlock<com.reiko.cars.road.StreetLampBlock> STREET_LAMP = BLOCKS.register("street_lamp",
            () -> new com.reiko.cars.road.StreetLampBlock(streetMetal()
                    .lightLevel(state -> state.getValue(com.reiko.cars.road.StreetLampBlock.LIT) ? 15 : 0)));
    public static final DeferredBlock<com.reiko.cars.traffic.TrafficLightBlock> TRAFFIC_LIGHT = BLOCKS.register("traffic_light",
            () -> new com.reiko.cars.traffic.TrafficLightBlock(streetMetal().strength(2.5F, 1200.0F).lightLevel(state -> 6)
                    .pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<com.reiko.cars.traffic.SpeedSignBlock> SPEED_SIGN = BLOCKS.register("speed_sign",
            () -> new com.reiko.cars.traffic.SpeedSignBlock(streetMetal().strength(2.5F, 1200.0F).pushReaction(PushReaction.BLOCK)));

    private ModBlocks() {
    }
}
