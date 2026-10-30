package com.reiko.cars.rental;

import com.reiko.cars.car.CarDeliveries;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Car rentals. A rental desk's owner parks their cars within {@code rentals.lotRadius} blocks of it: that's the fleet.
 * A customer picks one at the desk and rents it for 1 to {@code rentals.maxDays} Minecraft days, paying the day rate
 * for that kind of car (plus sales tax) by card or cash to the owner. While it's rented, the renter can open and drive
 * it as if they had its key; nobody else can. Bringing it back to the lot and getting out ends the rental early. When
 * the time's up the renter can't start it any more, and once nobody's in it, it goes back to its spot on the lot by
 * itself, wherever it was left.
 */
public final class RentalService {
    /** Warnings before a rental runs out, in ticks: two minutes and thirty seconds. */
    static final long FIRST_WARNING = 2_400;
    static final long LAST_WARNING = 600;
    private static final long DAY = 24_000;

    private RentalService() {
    }

    public static double lotRadius() {
        return CarsConfig.get(CarsConfig.RENTAL_LOT_RADIUS);
    }

    public static int maxDays() {
        return CarsConfig.get(CarsConfig.RENTAL_MAX_DAYS);
    }

    private static double flat(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    /** Whether {@code car} is on {@code desk}'s lot. */
    static boolean onLot(CarEntity car, BlockPos desk) {
        return flat(car.position(), desk.getCenter()) <= lotRadius() && Math.abs(car.getY() - desk.getY()) <= 6;
    }

    /** The desk's cars that can be rented now: its owner's, on the lot, parked, empty, not rented, sold or held. */
    public static List<CarEntity> fleet(RentalDeskBlockEntity desk) {
        Level level = desk.getLevel();
        if (level == null || desk.owner() == null) {
            return List.of();
        }
        BlockPos pos = desk.getBlockPos();
        Vec3 center = pos.getCenter();
        return level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(lotRadius(), 6.0, lotRadius()),
                        car -> car.isAlive() && desk.owner().equals(car.owner()) && !car.isPassenger() && car.getPassengers().isEmpty()
                                && car.rental() == null && !car.isForSale() && !car.isImpounded() && onLot(car, pos))
                .stream()
                .sorted(Comparator.<CarEntity>comparingDouble(car -> flat(car.position(), center)).thenComparingInt(Entity::getId))
                .toList();
    }

    /** The desk's cars out on rent (that are loaded), for its owner. */
    public static List<CarEntity> rentedOut(RentalDeskBlockEntity desk) {
        Level level = desk.getLevel();
        if (level == null) {
            return List.of();
        }
        BlockPos pos = desk.getBlockPos();
        return level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(128.0, 64.0, 128.0),
                car -> car.isAlive() && car.rental() != null && car.rental().desk().equals(pos)).stream().toList();
    }

    /** What renting a {@code type} for {@code days} costs at {@code desk}, before tax. */
    public static long cost(RentalDeskBlockEntity desk, CarType type, int days) {
        return desk.price(type) * days;
    }

    /**
     * {@code renter} rents {@code car} from {@code desk} for {@code days}, paying by card or cash.
     *
     * @param expected the cost the screen showed, before tax
     */
    public static ShopActionResult rent(ServerPlayer renter, RentalDeskBlockEntity desk, CarEntity car, int days, long expected, Payment payment) {
        if (desk.owner() == null) {
            return ShopActionResult.fail("rental.reikocars.no_owner");
        }
        if (!fleet(desk).contains(car)) {
            return ShopActionResult.fail("rental.reikocars.not_available");
        }
        if (desk.isOwner(renter)) {
            return ShopActionResult.fail("rental.reikocars.own_fleet");
        }
        if (days < 1 || days > maxDays()) {
            return ShopActionResult.fail("rental.reikocars.bad_days", maxDays());
        }
        EconomyApi eco = EconomyApi.get();
        long price = desk.price(car.carType());
        if (price <= 0) {
            return ShopActionResult.fail("rental.reikocars.not_renting", car.kindName());
        }
        long cost = price * days;
        if (cost != expected) {
            return ShopActionResult.fail("rental.reikocars.price_changed", eco.format(price));
        }
        long tax = EconomyConfig.taxRates().salesTax(cost);
        if (cost + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("rental.reikocars.too_expensive");
        }
        Component line = Component.translatable("rental.reikocars.bill_line", car.kindName(), days, desk.ownerName());
        ShopPayments.Charge charge = ShopPayments.charge(renter, payment, cost, tax, desk.owner(), "reikocars.car_rental", line);
        if (!charge.success()) {
            return ShopActionResult.fail(charge.message());
        }
        long until = renter.level().getGameTime() + days * DAY;
        car.setRental(new Rental(renter.getUUID(), renter.getGameProfile().getName(), desk.getBlockPos(), car.position(), car.getYRot(), until, false));
        car.setLocked(true);
        desk.recordRental(cost);
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
        ServerPlayer owner = renter.server.getPlayerList().getPlayer(desk.owner());
        if (owner != null && owner != renter) {
            owner.sendSystemMessage(Component.translatable("rental.reikocars.owner_rented", renter.getGameProfile().getName(), car.kindName(), days,
                    eco.format(cost)));
        }
        Component done = Component.translatable("rental.reikocars.rented", car.kindName(), days, eco.format(cost + tax));
        return ShopActionResult.ok(ShopPayments.receipt(done, payment, charge));
    }

    /** The owner sets a day's rent for a {@code type} (0 to stop renting them out). */
    public static ShopActionResult setPrice(ServerPlayer player, RentalDeskBlockEntity desk, CarType type, String text) {
        if (!desk.canManage(player) || desk.owner() == null) {
            return ShopActionResult.fail("rental.reikocars.owner_only", desk.ownerName());
        }
        OptionalLong price = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (price.isEmpty() || price.getAsLong() < 0 || price.getAsLong() > Money.MAX_BALANCE / 1000) {
            return ShopActionResult.fail("rental.reikocars.bad_price");
        }
        desk.setPrice(type, price.getAsLong());
        Component name = Component.translatable("entity.reikocars." + type.id());
        return ShopActionResult.ok(price.getAsLong() > 0
                ? Component.translatable("rental.reikocars.price_set", name, EconomyApi.get().format(price.getAsLong()))
                : Component.translatable("rental.reikocars.price_off", name));
    }

    /** "1d 4h" or "35m" of rental left, from {@code ticks}: a Minecraft day is 20 minutes. */
    public static Component timeLeft(long ticks) {
        long minutes = Math.max(0, (ticks + 1_199) / 1_200);
        return minutes >= 60 ? Component.translatable("rental.reikocars.hours_minutes", minutes / 60, minutes % 60)
                : Component.translatable("rental.reikocars.minutes", minutes);
    }

    // ---- every second, on the server, for a rented car ----

    public static void carTick(CarEntity car) {
        Rental rental = car.rental();
        if (rental == null) {
            return;
        }
        MinecraftServer server = car.getServer();
        long now = car.level().getGameTime();
        ServerPlayer renter = server == null ? null : server.getPlayerList().getPlayer(rental.renter());
        boolean empty = car.getPassengers().stream().noneMatch(passenger -> passenger instanceof net.minecraft.world.entity.player.Player);
        if (rental.active(now)) {
            boolean onLot = onLot(car, rental.desk());
            if (!rental.leftLot() && !onLot) {
                car.setRental(rental.leaving());
            } else if (rental.leftLot() && onLot && empty && car.standingStill()) {
                // Brought back early: that's it done.
                car.setRental(null);
                if (renter != null) {
                    renter.sendSystemMessage(Component.translatable("rental.reikocars.returned_early", car.kindName()));
                }
                return;
            }
            long left = rental.until() - now;
            if (renter != null && (left <= FIRST_WARNING && left > FIRST_WARNING - 20 || left <= LAST_WARNING && left > LAST_WARNING - 20)) {
                renter.sendSystemMessage(Component.translatable("rental.reikocars.running_out", car.kindName(), timeLeft(left)));
            }
            return;
        }
        if (now - rental.until() < 20 && renter != null) {
            renter.sendSystemMessage(Component.translatable("rental.reikocars.time_up", car.kindName()));
        }
        if (!empty || car.isPassenger()) {
            return; // still sitting in it (or on a tow truck): it goes back once it's left alone
        }
        car.setRental(null);
        if (renter != null && now - rental.until() >= 20) {
            renter.sendSystemMessage(Component.translatable("rental.reikocars.gone_back", car.kindName()));
        }
        if (!onLot(car, rental.desk())) {
            CarDeliveries.send(car, car.level().dimension(), rental.home(), rental.homeYaw());
        }
    }
}
