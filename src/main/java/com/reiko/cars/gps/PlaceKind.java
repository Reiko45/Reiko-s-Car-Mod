package com.reiko.cars.gps;

import com.reiko.cars.charge.ChargingStationBlockEntity;
import com.reiko.cars.dmv.DmvOfficeBlockEntity;
import com.reiko.cars.impound.ImpoundOfficeBlockEntity;
import com.reiko.cars.insurance.InsuranceOfficeBlockEntity;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import com.reiko.cars.pump.GasPumpBlockEntity;
import com.reiko.cars.racing.RaceControlBlockEntity;
import com.reiko.cars.registry.ModBlocks;
import com.reiko.cars.rental.RentalDeskBlockEntity;
import com.reiko.cars.repair.RepairBenchBlockEntity;
import com.reiko.cars.sale.DealershipBlockEntity;
import com.reiko.cars.tuning.TuningBenchBlockEntity;
import com.reiko.cars.wash.CarWashBlockEntity;
import com.reiko.cars.wash.WashBayBlockEntity;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/** The kinds of place the GPS can take you to: the mod's businesses and offices. */
public enum PlaceKind {
    GAS(0xFFE53935, () -> ModBlocks.GAS_PUMP),
    CHARGER(0xFF29B6F6, () -> ModBlocks.CHARGING_STATION),
    MECHANIC(0xFFFFA726, () -> ModBlocks.REPAIR_BENCH),
    CAR_WASH(0xFF4DD0E1, () -> ModBlocks.CAR_WASH),
    DEALERSHIP(0xFF66BB6A, () -> ModBlocks.DEALERSHIP),
    RENTAL(0xFFBA68C8, () -> ModBlocks.RENTAL_DESK),
    TUNING(0xFFFF7043, () -> ModBlocks.TUNING_BENCH),
    PARKING(0xFF90A4AE, () -> ModBlocks.PARKING_METER),
    DMV(0xFF7986CB, () -> ModBlocks.DMV_OFFICE),
    INSURANCE(0xFF4DB6AC, () -> ModBlocks.INSURANCE_OFFICE),
    IMPOUND(0xFFA1887F, () -> ModBlocks.IMPOUND_OFFICE),
    RACE(0xFFFFEE58, () -> ModBlocks.RACE_CONTROL);

    /** Its color in the GPS's lists. */
    public final int color;
    private final Supplier<ItemLike> icon;
    private final String id = name().toLowerCase(Locale.ROOT);

    PlaceKind(int color, Supplier<ItemLike> icon) {
        this.color = color;
        this.icon = icon;
    }

    public String id() {
        return id;
    }

    /** "Gas station", "Mechanic", "DMV" and so on. */
    public Component displayName() {
        return Component.translatable("gps.reikocars.place." + id);
    }

    /** The block it's found at, as an item for the GPS's lists. */
    public ItemStack icon() {
        return new ItemStack(icon.get());
    }

    /** The kind of place {@code entity} makes, or null if it isn't one. */
    @Nullable
    public static PlaceKind of(BlockEntity entity) {
        return entity instanceof GasPumpBlockEntity ? GAS
                : entity instanceof ChargingStationBlockEntity ? CHARGER
                : entity instanceof RepairBenchBlockEntity ? MECHANIC
                : entity instanceof CarWashBlockEntity || entity instanceof WashBayBlockEntity ? CAR_WASH
                : entity instanceof DealershipBlockEntity ? DEALERSHIP
                : entity instanceof RentalDeskBlockEntity ? RENTAL
                : entity instanceof TuningBenchBlockEntity ? TUNING
                : entity instanceof ParkingMeterBlockEntity ? PARKING
                : entity instanceof DmvOfficeBlockEntity ? DMV
                : entity instanceof InsuranceOfficeBlockEntity ? INSURANCE
                : entity instanceof ImpoundOfficeBlockEntity ? IMPOUND
                : entity instanceof RaceControlBlockEntity ? RACE
                : null;
    }
}
