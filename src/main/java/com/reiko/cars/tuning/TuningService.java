package com.reiko.cars.tuning;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.registry.ModItems;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What a Tuning Bench does. Only a car's owner (or an admin; anyone, for nobody's car) may have it tuned. Fitting
 * your own kit is free; the bench fits kits from its stock at its owner's prices (plus sales tax, by card or cash),
 * free for its owner. A set of tires or springs fitted over another comes off and is handed back. The race number
 * and stripes cost nothing.
 */
public final class TuningService {
    private TuningService() {
    }

    public static double reach() {
        return CarsConfig.get(CarsConfig.TUNING_REACH);
    }

    @Nullable
    public static TuningKit kitOf(ItemStack stack) {
        return stack.getItem() instanceof TuningKitItem item ? item.kit() : null;
    }

    public static ItemStack stackOf(TuningKit kit) {
        return new ItemStack(ModItems.kit(kit));
    }

    /** Every car within reach of the bench, nearest first. */
    public static List<CarEntity> carsInReach(TuningBenchBlockEntity bench) {
        Level level = bench.getLevel();
        if (level == null) {
            return List.of();
        }
        Vec3 center = bench.getBlockPos().getCenter();
        double reach = reach();
        return level.getEntitiesOfClass(CarEntity.class, new AABB(bench.getBlockPos()).inflate(reach + 2.0),
                        car -> inReach(bench, car))
                .stream().sorted(Comparator.<CarEntity>comparingDouble(car -> car.getBoundingBox().distanceToSqr(center)).thenComparingInt(Entity::getId))
                .toList();
    }

    public static boolean inReach(TuningBenchBlockEntity bench, CarEntity car) {
        return car.isAlive() && !car.isPassenger() && car.level() == bench.getLevel()
                && Math.sqrt(car.getBoundingBox().distanceToSqr(bench.getBlockPos().getCenter())) <= reach();
    }

    /** Whether {@code player} may have {@code car} tuned: its owner, an admin, or anyone for nobody's car. */
    public static boolean mayTune(Player player, CarEntity car) {
        return car.owner() == null || car.isOwner(player) || player.hasPermissions(2);
    }

    /** How many of {@code kit} {@code player} carries. */
    public static int carried(Player player, TuningKit kit) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (kitOf(stack) == kit) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean takeCarried(Player player, TuningKit kit) {
        for (ItemStack stack : player.getInventory().items) {
            if (kitOf(stack) == kit) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void hand(Player player, @Nullable TuningKit kit) {
        if (kit != null) {
            ItemStack back = stackOf(kit);
            if (!player.getInventory().add(back)) {
                player.drop(back, false);
            }
        }
    }

    @Nullable
    private static ShopActionResult checkCar(ServerPlayer player, TuningBenchBlockEntity bench, @Nullable CarEntity car) {
        if (car == null || !inReach(bench, car)) {
            return ShopActionResult.fail("tuning.reikocars.no_car", (int) reach());
        }
        if (!mayTune(player, car)) {
            return ShopActionResult.fail("tuning.reikocars.not_yours", car.kindName(), car.ownerName());
        }
        return null;
    }

    /** {@code player} fits a {@code kit} of their own to {@code car}, for nothing. */
    public static ShopActionResult fitOwn(ServerPlayer player, TuningBenchBlockEntity bench, @Nullable CarEntity car, TuningKit kit) {
        ShopActionResult refused = checkCar(player, bench, car);
        if (refused != null) {
            return refused;
        }
        if (!kit.suits(car.carType())) {
            return ShopActionResult.fail(kit == TuningKit.BATTERY ? "tuning.reikocars.unsuited_battery" : "tuning.reikocars.unsuited",
                    kit.displayName(), car.kindName());
        }
        Upgrades now = car.upgrades();
        Upgrades next = kit.fit(now);
        if (next == null) {
            return ShopActionResult.fail("tuning.reikocars.maxed", kit.displayName());
        }
        if (!takeCarried(player, kit)) {
            return ShopActionResult.fail("tuning.reikocars.none_carried", kit.displayName());
        }
        hand(player, kit.replaces(now));
        car.setUpgrades(next);
        fitted(car);
        return ShopActionResult.ok(Component.translatable("tuning.reikocars.fitted", kit.displayName(), car.kindName()));
    }

    /** The bench fits the kits picked in {@code mask} (a bit per {@link TuningKit}) from its stock, for {@code expected} before tax. */
    public static ShopActionResult buy(ServerPlayer player, TuningBenchBlockEntity bench, @Nullable CarEntity car, int mask, long expected,
                                       Payment payment) {
        ShopActionResult refused = checkCar(player, bench, car);
        if (refused != null) {
            return refused;
        }
        List<TuningKit> kits = new ArrayList<>();
        for (TuningKit kit : TuningKit.values()) {
            if ((mask & 1 << kit.ordinal()) != 0) {
                kits.add(kit);
            }
        }
        if (kits.isEmpty()) {
            return ShopActionResult.fail("tuning.reikocars.nothing_picked");
        }
        if (kits.contains(TuningKit.SPORT_TIRES) && kits.contains(TuningKit.OFF_ROAD_TIRES)
                || kits.contains(TuningKit.LOWERING) && kits.contains(TuningKit.LIFT)) {
            return ShopActionResult.fail("tuning.reikocars.one_or_other");
        }
        Upgrades start = car.upgrades();
        Upgrades next = start;
        long total = 0;
        List<TuningKit> off = new ArrayList<>();
        for (TuningKit kit : kits) {
            if (!kit.suits(car.carType())) {
                return ShopActionResult.fail(kit == TuningKit.BATTERY ? "tuning.reikocars.unsuited_battery" : "tuning.reikocars.unsuited",
                        kit.displayName(), car.kindName());
            }
            Upgrades fitted = kit.fit(next);
            if (fitted == null) {
                return ShopActionResult.fail("tuning.reikocars.maxed", kit.displayName());
            }
            if (bench.count(kit) <= 0) {
                return ShopActionResult.fail("tuning.reikocars.out_of_stock", kit.displayName());
            }
            TuningKit replaced = kit.replaces(next);
            if (replaced != null) {
                off.add(replaced);
            }
            next = fitted;
            total += bench.price(kit);
        }
        if (total != expected) {
            return ShopActionResult.fail("tuning.reikocars.total_changed", EconomyApi.get().format(total + EconomyConfig.taxRates().salesTax(total)));
        }
        boolean free = bench.isOwner(player) || total == 0;
        ShopPayments.Charge charge = null;
        if (!free) {
            long tax = EconomyConfig.taxRates().salesTax(total);
            if (total + tax > Money.MAX_BALANCE) {
                return ShopActionResult.fail("tuning.reikocars.too_expensive");
            }
            Component line = Component.translatable("tuning.reikocars.bill_line",
                    ComponentUtils.formatList(kits.stream().map(TuningKit::displayName).toList(), Component.literal(", ")), bench.ownerName());
            charge = ShopPayments.charge(player, payment, total, tax, bench.owner(), "reikocars.tuning", line);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        for (TuningKit kit : kits) {
            bench.take(kit, 1);
        }
        off.forEach(kit -> hand(player, kit));
        car.setUpgrades(next);
        fitted(car);
        if (!free) {
            bench.recordJob(total);
        }
        // The bench's kits on someone else's car: its owner's work as a mechanic, if they're on.
        if (bench.owner() != null && !bench.owner().equals(car.owner())) {
            CarJobs.work(player.server.getPlayerList().getPlayer(bench.owner()), CarJobs.REPAIR_PART, kits.size());
        }
        Component done = Component.translatable("tuning.reikocars.bought",
                ComponentUtils.formatList(kits.stream().map(TuningKit::displayName).toList(), Component.literal(", ")), car.kindName());
        return ShopActionResult.ok(charge == null ? done : ShopPayments.receipt(done, payment, charge));
    }

    private static void fitted(CarEntity car) {
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    /** The bench's owner sets what fitting a {@code kit} from its stock costs. */
    public static ShopActionResult setPrice(ServerPlayer player, TuningBenchBlockEntity bench, TuningKit kit, String text) {
        if (!bench.canManage(player)) {
            return ShopActionResult.fail("tuning.reikocars.owner_only", bench.ownerName());
        }
        OptionalLong price = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (price.isEmpty() || price.getAsLong() < 0 || price.getAsLong() > Money.MAX_BALANCE / 10) {
            return ShopActionResult.fail("tuning.reikocars.bad_price");
        }
        bench.setPrice(kit, price.getAsLong());
        return ShopActionResult.ok(Component.translatable("tuning.reikocars.price_set", kit.displayName(), EconomyApi.get().format(price.getAsLong())));
    }

    /** {@code player} paints {@code car} with race number {@code number} (0 for none) and {@code stripes} (null for none). */
    public static ShopActionResult setLivery(ServerPlayer player, TuningBenchBlockEntity bench, @Nullable CarEntity car, int number,
                                             @Nullable DyeColor stripes) {
        ShopActionResult refused = checkCar(player, bench, car);
        if (refused != null) {
            return refused;
        }
        if (number < 0 || number > Livery.MAX_NUMBER) {
            return ShopActionResult.fail("tuning.reikocars.bad_number", Livery.MAX_NUMBER);
        }
        car.setLivery(Livery.of(number, stripes));
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        return ShopActionResult.ok(Component.translatable("tuning.reikocars.livery_set", car.kindName()));
    }
}
