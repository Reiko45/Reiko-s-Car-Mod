package com.reiko.cars.pump;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarFuel;
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
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Selling fuel. Payment goes through Reiko's Economy Mod exactly as a shop purchase does: a debit card (with PIN)
 * pays from its bank account, a credit card puts it on the bill, cash is taken with change given back, sales tax
 * is added, and the pump's owner is paid into their bank account. The owner fills up for free.
 */
public final class PumpService {
    /** Pumps sell in tenths of a bucket. */
    public static final int STEP = 100;

    private PumpService() {
    }

    /** Price of {@code millibuckets} at {@code pricePerBucket} cents, rounded up to the cent. */
    public static long cost(long pricePerBucket, int millibuckets) {
        return Math.ceilDiv(pricePerBucket * millibuckets, 1000L);
    }

    /** The car {@code player} would fill at this pump: the one they're in, or else the nearest they may use. */
    @Nullable
    public static CarEntity findCar(GasPumpBlockEntity pump, Player player) {
        Level level = pump.getLevel();
        if (level == null) {
            return null;
        }
        if (player.getVehicle() instanceof CarEntity riding && inReach(pump, riding)) {
            return riding;
        }
        double hose = CarsConfig.get(CarsConfig.HOSE_LENGTH);
        Vec3 center = pump.getBlockPos().getCenter();
        List<CarEntity> cars = level.getEntitiesOfClass(CarEntity.class, new AABB(pump.getBlockPos()).inflate(hose + 2.0),
                car -> car.isAlive() && car.canBeUsedBy(player) && inReach(pump, car));
        return cars.stream().min(Comparator.comparingDouble(car -> distance(center, car))).orElse(null);
    }

    public static boolean inReach(GasPumpBlockEntity pump, CarEntity car) {
        return car.isAlive() && car.level() == pump.getLevel()
                && distance(pump.getBlockPos().getCenter(), car) <= CarsConfig.get(CarsConfig.HOSE_LENGTH);
    }

    /** From the pump to the nearest point of the car. */
    private static double distance(Vec3 from, CarEntity car) {
        AABB box = car.getBoundingBox();
        double dx = Math.max(Math.max(box.minX - from.x, 0.0), from.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - from.y, 0.0), from.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - from.z, 0.0), from.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Millibuckets of the pump's fuel that fit in the car's tank. */
    public static int room(GasPumpBlockEntity pump, CarEntity car) {
        return CarFuel.millibucketsThatFit(pump.fuel(), car.fuelRoom());
    }

    /**
     * Pumps up to {@code requested} millibuckets into {@code car} and charges {@code player} for it. Nothing moves
     * unless the payment goes through.
     *
     * @param expectedPrice the price per bucket the player saw, so a price change in the meantime is caught
     */
    public static ShopActionResult fill(ServerPlayer player, GasPumpBlockEntity pump, @Nullable CarEntity car, int requested,
                                        long expectedPrice, Payment payment) {
        if (car == null || !inReach(pump, car)) {
            return ShopActionResult.fail("pump.reikocars.no_car", (int) CarsConfig.get(CarsConfig.HOSE_LENGTH));
        }
        FluidStack inPump = pump.fuel();
        if (inPump.isEmpty()) {
            return ShopActionResult.fail("pump.reikocars.empty");
        }
        if (pump.price() != expectedPrice) {
            return ShopActionResult.fail("pump.reikocars.price_changed", EconomyApi.get().format(pump.price()));
        }
        int room = room(pump, car);
        if (room <= 0) {
            return ShopActionResult.fail("pump.reikocars.car_full");
        }
        int amount = Math.min(Math.min(requested, inPump.getAmount()), room);
        if (amount <= 0) {
            return ShopActionResult.fail("pump.reikocars.nothing_to_pump");
        }

        long total = cost(pump.price(), amount);
        boolean free = pump.isOwner(player) || total == 0;
        ShopPayments.Charge charge = null;
        long tax = 0;
        if (!free) {
            tax = EconomyConfig.taxRates().salesTax(total);
            if (total + tax > Money.MAX_BALANCE) {
                return ShopActionResult.fail("pump.reikocars.too_expensive");
            }
            Component description = Component.translatable("pump.reikocars.bill_line", buckets(amount),
                    inPump.getHoverName(), pump.ownerName());
            charge = ShopPayments.charge(player, payment, total, tax, pump.owner(), "reikocars.fuel", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }

        FluidStack pumped = pump.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
        car.addFuel(CarFuel.energyOf(pumped, pumped.getAmount()));
        if (!free) {
            pump.recordSale(pumped.getAmount(), total);
        }
        player.level().playSound(null, pump.getBlockPos(), ModSounds.PUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);

        Component line = free
                ? Component.translatable("pump.reikocars.pumped_free", buckets(amount), pumped.getHoverName())
                : tax > 0
                        ? Component.translatable("pump.reikocars.pumped_tax", buckets(amount), pumped.getHoverName(),
                                EconomyApi.get().format(total + tax), EconomyApi.get().format(tax))
                        : Component.translatable("pump.reikocars.pumped", buckets(amount), pumped.getHoverName(),
                                EconomyApi.get().format(total));
        return ShopActionResult.ok(free ? line : ShopPayments.receipt(line, payment, charge));
    }

    /** Sets the price per bucket from typed text like {@code 3.75}. Owner or admin only. */
    public static ShopActionResult setPrice(ServerPlayer player, GasPumpBlockEntity pump, String text) {
        if (!pump.canManage(player)) {
            return ShopActionResult.fail("pump.reikocars.owner_only_price", pump.ownerName());
        }
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 1000) {
            return ShopActionResult.fail("pump.reikocars.bad_price");
        }
        pump.setPrice(parsed.getAsLong());
        return ShopActionResult.ok(Component.translatable("pump.reikocars.price_set", EconomyApi.get().format(parsed.getAsLong())));
    }

    /** "4.5" for 4,500 millibuckets. */
    public static String buckets(int millibuckets) {
        return millibuckets % 1000 == 0 ? Integer.toString(millibuckets / 1000) : String.format(java.util.Locale.ROOT, "%.1f", millibuckets / 1000.0);
    }
}
