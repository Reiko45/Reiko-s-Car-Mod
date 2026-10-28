package com.reiko.cars.pump;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarFuel;
import com.reiko.cars.car.FuelType;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Selling fuel, into a car or into the empty buckets a player carries. Payment goes through Reiko's Economy Mod
 * exactly as a shop purchase does: a debit card (with PIN) pays from its bank account, a credit card puts it on the
 * bill, cash is taken with change given back, sales tax is added, and the pump's owner is paid into their bank
 * account. The owner fills up for free.
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

    /** Millibuckets of the pump's fuel that fit in the car's tank: none if the car doesn't run on it. */
    public static int room(GasPumpBlockEntity pump, CarEntity car) {
        return runsOn(pump, car) ? CarFuel.millibucketsThatFit(pump.fuel(), car.fuelRoom()) : 0;
    }

    /** Whether the car runs on what the pump sells. */
    public static boolean runsOn(GasPumpBlockEntity pump, CarEntity car) {
        return car.carType().fuel() == sells(pump);
    }

    /**
     * The kind of fuel in the pump, or that it would take when empty. Usually the pump's own kind, but a pump from
     * before there were diesel pumps can still hold diesel.
     */
    public static FuelType sells(GasPumpBlockEntity pump) {
        FuelType inTank = FuelType.of(pump.fuel());
        return inTank != null ? inTank : pump.fuelType();
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
        if (!runsOn(pump, car)) {
            return ShopActionResult.fail("pump.reikocars.wrong_car_fuel", car.getDisplayName(), car.carType().fuel().displayName(),
                    sells(pump).displayName());
        }
        int room = room(pump, car);
        if (room <= 0) {
            return ShopActionResult.fail("pump.reikocars.car_full");
        }
        int amount = Math.min(Math.min(requested, inPump.getAmount()), room);
        if (amount <= 0) {
            return ShopActionResult.fail("pump.reikocars.nothing_to_pump");
        }
        Bill bill = Bill.charge(player, pump, inPump, amount, payment);
        if (bill.failure != null) {
            return bill.failure;
        }

        FluidStack pumped = pump.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
        car.addFuel(CarFuel.energyOf(pumped, pumped.getAmount()));
        return bill.finish(player, pump, pumped, payment, "pump.reikocars.pumped");
    }

    /**
     * Fills up to {@code requested} of the empty buckets {@code player} carries from the pump and charges them for
     * it. Nothing moves unless the payment goes through.
     *
     * @param expectedPrice the price per bucket the player saw, so a price change in the meantime is caught
     */
    public static ShopActionResult fillBuckets(ServerPlayer player, GasPumpBlockEntity pump, int requested, long expectedPrice,
                                               Payment payment) {
        FluidStack inPump = pump.fuel();
        if (inPump.isEmpty()) {
            return ShopActionResult.fail("pump.reikocars.empty");
        }
        if (pump.price() != expectedPrice) {
            return ShopActionResult.fail("pump.reikocars.price_changed", EconomyApi.get().format(pump.price()));
        }
        ItemStack filled = FluidUtil.getFilledBucket(new FluidStack(inPump.getFluid(), FluidType.BUCKET_VOLUME));
        if (filled.isEmpty()) {
            return ShopActionResult.fail("pump.reikocars.no_bucket_for", inPump.getHoverName());
        }
        int carried = emptyBuckets(player);
        if (carried <= 0) {
            return ShopActionResult.fail("pump.reikocars.no_empty_buckets");
        }
        int count = Math.min(Math.min(requested, carried), inPump.getAmount() / FluidType.BUCKET_VOLUME);
        if (count <= 0) {
            return ShopActionResult.fail(requested <= 0 ? "pump.reikocars.nothing_to_pump" : "pump.reikocars.less_than_a_bucket");
        }
        int amount = count * FluidType.BUCKET_VOLUME;
        Bill bill = Bill.charge(player, pump, inPump, amount, payment);
        if (bill.failure != null) {
            return bill.failure;
        }

        FluidStack pumped = pump.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE);
        takeEmptyBuckets(player, count);
        for (int i = 0; i < count; i++) {
            ItemStack bucket = filled.copy();
            if (!player.getInventory().add(bucket)) {
                player.drop(bucket, false);
            }
        }
        // The pump screen has no slots, so send the inventory now rather than when the screen closes.
        player.inventoryMenu.broadcastChanges();
        return bill.finish(player, pump, pumped, payment, "pump.reikocars.bucketed");
    }

    /** Empty buckets in {@code player}'s inventory. */
    public static int emptyBuckets(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.BUCKET)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void takeEmptyBuckets(Player player, int count) {
        for (ItemStack stack : player.getInventory().items) {
            if (count <= 0) {
                break;
            }
            if (stack.is(Items.BUCKET)) {
                int taken = Math.min(count, stack.getCount());
                stack.shrink(taken);
                count -= taken;
            }
        }
        player.getInventory().setChanged();
    }

    /** The money side of a sale: free for the owner, otherwise a charge through the economy mod. */
    private static final class Bill {
        private final long total;
        private final long tax;
        private final boolean free;
        @Nullable
        private final ShopPayments.Charge charge;
        @Nullable
        private final ShopActionResult failure;

        private Bill(long total, long tax, boolean free, @Nullable ShopPayments.Charge charge, @Nullable ShopActionResult failure) {
            this.total = total;
            this.tax = tax;
            this.free = free;
            this.charge = charge;
            this.failure = failure;
        }

        /** Charges {@code player} for {@code amount} millibuckets of {@code fuel}; check {@link #failure} first. */
        static Bill charge(ServerPlayer player, GasPumpBlockEntity pump, FluidStack fuel, int amount, Payment payment) {
            long total = cost(pump.price(), amount);
            if (pump.isOwner(player) || total == 0) {
                return new Bill(total, 0, true, null, null);
            }
            long tax = EconomyConfig.taxRates().salesTax(total);
            if (total + tax > Money.MAX_BALANCE) {
                return new Bill(total, tax, false, null, ShopActionResult.fail("pump.reikocars.too_expensive"));
            }
            Component description = Component.translatable("pump.reikocars.bill_line", buckets(amount), fuel.getHoverName(), pump.ownerName());
            ShopPayments.Charge charge = ShopPayments.charge(player, payment, total, tax, pump.owner(), "reikocars.fuel", description);
            return new Bill(total, tax, false, charge, charge.success() ? null : ShopActionResult.fail(charge.message()));
        }

        /**
         * Records the sale, plays the pump sound and makes the receipt line, from {@code key} plus {@code _free},
         * {@code _tax} or nothing: "Pumped 3 B of Light Fuel for $12.00", "Filled 2 buckets with Dense Fuel ...".
         */
        ShopActionResult finish(ServerPlayer player, GasPumpBlockEntity pump, FluidStack sold, Payment payment, String key) {
            if (!free) {
                pump.recordSale(sold.getAmount(), total);
            }
            player.level().playSound(null, pump.getBlockPos(), ModSounds.PUMP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            String amount = buckets(sold.getAmount());
            Component line = free
                    ? Component.translatable(key + "_free", amount, sold.getHoverName())
                    : tax > 0
                            ? Component.translatable(key + "_tax", amount, sold.getHoverName(), EconomyApi.get().format(total + tax),
                                    EconomyApi.get().format(tax))
                            : Component.translatable(key, amount, sold.getHoverName(), EconomyApi.get().format(total));
            return ShopActionResult.ok(free ? line : ShopPayments.receipt(line, payment, charge));
        }
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
