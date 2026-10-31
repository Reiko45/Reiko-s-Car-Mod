package com.reiko.cars.charge;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModSounds;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Selling charge to electric cars. Payment goes through Reiko's Economy Mod exactly as at the gas pumps: a debit card
 * (with PIN) pays from its bank account, a credit card puts it on the bill, cash is taken with change given back, sales
 * tax is added, and the station's owner is paid into their bank account. The owner charges for free.
 */
public final class ChargerService {
    /** Stations sell in steps of 1,000 MJ. */
    public static final long STEP = 1_000;

    private ChargerService() {
    }

    /** Price of {@code mj} MJ at {@code pricePer1000} cents per 1,000 MJ, rounded up to the cent. */
    public static long cost(long pricePer1000, long mj) {
        return Math.ceilDiv(pricePer1000 * mj, 1000L);
    }

    /** The car {@code player} would charge here: the one they're in, or else the nearest electric one they may use. */
    @Nullable
    public static CarEntity findCar(ChargingStationBlockEntity station, Player player) {
        Level level = station.getLevel();
        if (level == null) {
            return null;
        }
        if (player.getVehicle() instanceof CarEntity riding && inReach(station, riding)) {
            return riding;
        }
        double cable = CarsConfig.get(CarsConfig.HOSE_LENGTH);
        Vec3 center = station.getBlockPos().getCenter();
        List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, new AABB(station.getBlockPos()).inflate(cable + 2.0),
                car -> car.isAlive() && car.canBeUsedBy(player) && inReach(station, car));
        // An electric car first, then the nearest of the rest (to say it doesn't run on electricity).
        return cars.stream().min(Comparator.<CarEntity>comparingInt(car -> car.carType().isElectric() ? 0 : 1)
                .thenComparingDouble(car -> distance(center, car))).orElse(null);
    }

    public static boolean inReach(ChargingStationBlockEntity station, CarEntity car) {
        return car.isAlive() && car.level() == station.getLevel()
                && distance(station.getBlockPos().getCenter(), car) <= CarsConfig.get(CarsConfig.HOSE_LENGTH);
    }

    /** From the station to the nearest point of the car. */
    private static double distance(Vec3 from, CarEntity car) {
        AABB box = car.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - from.x, 0.0), from.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - from.y, 0.0), from.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - from.z, 0.0), from.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Whole MJ the car's battery can still take: none if it isn't electric. */
    public static long room(CarEntity car) {
        return car.carType().isElectric() ? (long) Math.floor(car.fuelRoom() + 1.0E-6) : 0;
    }

    /**
     * Charges up to {@code requested} MJ into {@code car} and bills {@code player} for it. Nothing moves unless the
     * payment goes through.
     *
     * @param expectedPrice the price per 1,000 MJ the player saw, so a price change in the meantime is caught
     */
    public static ShopActionResult charge(ServerPlayer player, ChargingStationBlockEntity station, @Nullable CarEntity car, long requested,
                                          long expectedPrice, Payment payment) {
        if (car == null || !inReach(station, car)) {
            return ShopActionResult.fail("charge.reikocars.no_car", (int) CarsConfig.get(CarsConfig.HOSE_LENGTH));
        }
        if (station.price() != expectedPrice) {
            return ShopActionResult.fail("charge.reikocars.price_changed", EconomyApi.get().format(station.price()));
        }
        if (!car.carType().isElectric()) {
            return ShopActionResult.fail("charge.reikocars.not_electric", car.getDisplayName(), car.carType().fuel().displayName());
        }
        long stored = station.storedMj();
        if (stored <= 0) {
            return ShopActionResult.fail("charge.reikocars.no_power");
        }
        long room = room(car);
        if (room <= 0) {
            return ShopActionResult.fail("charge.reikocars.battery_full");
        }
        long amount = Math.min(Math.min(requested, stored), room);
        if (amount <= 0) {
            return ShopActionResult.fail("charge.reikocars.nothing_to_charge");
        }

        long total = cost(station.price(), amount);
        boolean free = station.isOwner(player) || total == 0;
        long tax = free ? 0 : EconomyConfig.taxRates().salesTax(total);
        ShopPayments.Charge bill = null;
        if (!free) {
            if (total + tax > Money.MAX_BALANCE) {
                return ShopActionResult.fail("charge.reikocars.too_expensive");
            }
            Component line = Component.translatable("charge.reikocars.bill_line", mj(amount), station.ownerName());
            bill = ShopPayments.charge(player, payment, total, tax, station.owner(), "reikocars.charge", line);
            if (!bill.success()) {
                return ShopActionResult.fail(bill.message());
            }
        }

        station.drainMj(amount);
        car.addFuel(amount);
        com.reiko.cars.advancement.CarAdvancements.award(player, "charge");
        if (!free) {
            station.recordSale(amount, total);
        }
        player.level().playSound(null, station.getBlockPos(), ModSounds.CHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        Component line = free
                ? Component.translatable("charge.reikocars.charged_free", mj(amount))
                : tax > 0
                        ? Component.translatable("charge.reikocars.charged_tax", mj(amount), EconomyApi.get().format(total + tax),
                                EconomyApi.get().format(tax))
                        : Component.translatable("charge.reikocars.charged", mj(amount), EconomyApi.get().format(total));
        return ShopActionResult.ok(free ? line : ShopPayments.receipt(line, payment, bill));
    }

    /** Sets the price per 1,000 MJ from typed text like {@code 0.05}. Owner or admin only. */
    public static ShopActionResult setPrice(ServerPlayer player, ChargingStationBlockEntity station, String text) {
        if (!station.canManage(player)) {
            return ShopActionResult.fail("charge.reikocars.owner_only_price", station.ownerName());
        }
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 1_000_000) {
            return ShopActionResult.fail("charge.reikocars.bad_price");
        }
        station.setPrice(parsed.getAsLong());
        return ShopActionResult.ok(Component.translatable("charge.reikocars.price_set", EconomyApi.get().format(parsed.getAsLong())));
    }

    /** "12,500" for 12,500 MJ. */
    public static String mj(long mj) {
        return String.format(Locale.ROOT, "%,d", mj);
    }

    /** "12.5k" for 12,500 MJ, short for small displays. */
    public static String kmj(long mj) {
        if (mj < 1000) {
            return Long.toString(mj);
        }
        if (mj >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", mj / 1_000_000.0);
        }
        return mj % 1000 == 0 ? (mj / 1000) + "k" : String.format(Locale.ROOT, "%.1fk", mj / 1000.0);
    }
}
