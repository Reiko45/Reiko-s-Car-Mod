package com.reiko.cars.parking;

import com.reiko.cars.car.CarEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Parking meters. The car parked at a meter (the nearest within {@link #REACH} blocks, standing still and not being
 * carried) can have time paid for at the meter's rate, by anyone, by card or cash plus sales tax, paid to the meter's
 * owner. A car left there unpaid, or after its time's run out, is ticketed after a grace period: the fine comes out of
 * its owner's bank account (as much as they have) and goes to the meter's owner, and again every so often while it
 * stays. The meter owner's own cars park free.
 */
public final class MeterService {
    /** How close, across the ground, a car must be to a meter to be parked at it, in blocks. */
    public static final double REACH = 3.0;
    /** The most time that can be bought in one go, in minutes, and in all. */
    public static final int MAX_MINUTES = 120;
    public static final int MAX_PAID_MINUTES = 240;

    private MeterService() {
    }

    /** The car parked at {@code meter}, if any. */
    @Nullable
    public static CarEntity parkedAt(ParkingMeterBlockEntity meter) {
        Level level = meter.getLevel();
        if (level == null) {
            return null;
        }
        Vec3 center = meter.getBlockPos().getCenter();
        CarEntity car = level.getEntitiesOfClass(CarEntity.class, new AABB(meter.getBlockPos()).inflate(REACH + 6.0, 2.0, REACH + 6.0),
                        c -> c.isAlive() && !c.isPassenger() && flat(c, center) <= REACH)
                .stream().min(Comparator.comparingDouble(c -> flat(c, center))).orElse(null);
        // A car between two meters belongs to the nearer one, so paying at one doesn't get it a ticket from the other.
        return car == null || nearestMeter(car) == meter ? car : null;
    }

    /** The meter {@code car} is parked at: the nearest within reach, if any. */
    @Nullable
    public static ParkingMeterBlockEntity nearestMeter(CarEntity car) {
        Level level = car.level();
        int reach = (int) Math.ceil(car.carType().length / 2.0 + REACH) + 1;
        BlockPos at = car.blockPosition();
        ParkingMeterBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-reach, -2, -reach), at.offset(reach, 2, reach))) {
            if (level.getBlockEntity(pos) instanceof ParkingMeterBlockEntity meter) {
                double distance = flat(car, pos.getCenter());
                // ties go to the meter with the smaller position, so every meter agrees
                if (distance <= REACH && (distance < bestDistance - 1e-6
                        || (Math.abs(distance - bestDistance) <= 1e-6 && best != null && pos.compareTo(best.getBlockPos()) < 0))) {
                    bestDistance = distance;
                    best = meter;
                }
            }
        }
        return best;
    }

    /** How far {@code car}'s nearest side is from {@code point}, across the ground. */
    private static double flat(CarEntity car, Vec3 point) {
        // A car is long: measure to the nearest point on its centre line, not its middle.
        Vec3 forward = car.forward();
        double half = car.carType().length / 2.0;
        double dx = point.x - car.getX();
        double dz = point.z - car.getZ();
        double along = Math.max(-half, Math.min(half, dx * forward.x + dz * forward.z));
        double nx = car.getX() + forward.x * along;
        double nz = car.getZ() + forward.z * along;
        return Math.max(0.0, Math.sqrt((point.x - nx) * (point.x - nx) + (point.z - nz) * (point.z - nz)) - car.carType().width / 2.0);
    }

    /** What {@code minutes} of parking costs at {@code meter}, before tax. */
    public static long cost(ParkingMeterBlockEntity meter, int minutes) {
        return meter.rate() * (minutes / 10);
    }

    /**
     * {@code payer} pays for {@code minutes} (in tens) for the car parked at {@code meter}.
     *
     * @param expected the cost the screen showed, before tax
     */
    public static ShopActionResult pay(ServerPlayer payer, ParkingMeterBlockEntity meter, int minutes, long expected, Payment payment) {
        CarEntity car = parkedAt(meter);
        if (car == null) {
            return ShopActionResult.fail("parking.reikocars.no_car");
        }
        if (minutes < 10 || minutes > MAX_MINUTES || minutes % 10 != 0) {
            return ShopActionResult.fail(Component.literal("?"));
        }
        Level level = meter.getLevel();
        long now = level.getGameTime();
        long already = meter.timeLeft(car, now);
        if (already + minutes * ParkingMeterBlockEntity.TEN_MINUTES / 10 > MAX_PAID_MINUTES * ParkingMeterBlockEntity.TEN_MINUTES / 10) {
            return ShopActionResult.fail("parking.reikocars.too_long", MAX_PAID_MINUTES / 60);
        }
        long cost = cost(meter, minutes);
        EconomyApi eco = EconomyApi.get();
        if (cost != expected) {
            return ShopActionResult.fail("parking.reikocars.price_changed", eco.format(cost + EconomyConfig.taxRates().salesTax(cost)));
        }
        long tax = cost > 0 && !meter.isOwner(payer) ? EconomyConfig.taxRates().salesTax(cost) : 0;
        ShopPayments.Charge charge = null;
        if (cost > 0 && !meter.isOwner(payer)) {
            if (cost + tax > Money.MAX_BALANCE) {
                return ShopActionResult.fail("parking.reikocars.bad_price");
            }
            Component description = Component.translatable("parking.reikocars.bill_line", minutes, car.kindName());
            charge = ShopPayments.charge(payer, payment, cost, tax, meter.owner(), "reikocars.parking", description);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
            meter.recordPayment(cost);
        }
        meter.addTime(car, minutes * ParkingMeterBlockEntity.TEN_MINUTES / 10, now);
        level.playSound(null, meter.getBlockPos(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.6F, 1.6F);
        Component line = charge == null
                ? Component.translatable("parking.reikocars.paid_free", minutes, car.kindName())
                : tax > 0 ? Component.translatable("parking.reikocars.paid_tax", minutes, car.kindName(), eco.format(cost + tax), eco.format(tax))
                : Component.translatable("parking.reikocars.paid", minutes, car.kindName(), eco.format(cost));
        return charge == null ? ShopActionResult.ok(line) : ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** A parking ticket for {@code car} at {@code meter}: the fine, from its owner's bank account to the meter's owner. */
    static void ticket(ParkingMeterBlockEntity meter, CarEntity car) {
        Level level = meter.getLevel();
        MinecraftServer server = level == null ? null : level.getServer();
        UUID payer = car.owner();
        UUID payee = meter.owner();
        if (server == null || payer == null || payee == null) {
            return;
        }
        EconomyApi eco = EconomyApi.get();
        long fine = meter.fine();
        long pay = Math.min(fine, Math.max(0, eco.getBalance(server, payer)));
        if (pay > 0) {
            TransactionResult result = eco.transfer(server, payer, payee, pay, "reikocars.parking_fine");
            if (!result.isSuccess()) {
                pay = 0;
            }
        }
        meter.recordTicket(pay);
        BlockPos at = meter.getBlockPos();
        level.playSound(null, at, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 0.6F);
        ServerPlayer driver = server.getPlayerList().getPlayer(payer);
        if (driver != null) {
            driver.sendSystemMessage(pay >= fine
                    ? Component.translatable("parking.reikocars.ticket", eco.format(fine), car.describe(), at.getX(), at.getY(), at.getZ())
                    : Component.translatable("parking.reikocars.ticket_short", eco.format(fine), car.describe(), at.getX(), at.getY(), at.getZ(), eco.format(pay)));
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(payee);
        if (owner != null && fine > 0) {
            owner.sendSystemMessage(Component.translatable("parking.reikocars.ticket_owner", car.ownerName(), car.describe(), eco.format(pay)));
        }
    }

    /** The meter's owner sets the rate per 10 minutes and the fine. */
    public static ShopActionResult setPrices(ServerPlayer player, ParkingMeterBlockEntity meter, String rateText, String fineText) {
        if (!meter.canManage(player) || meter.owner() == null) {
            return ShopActionResult.fail("parking.reikocars.owner_only", meter.ownerName());
        }
        OptionalLong rate = parse(rateText);
        OptionalLong fine = parse(fineText);
        if (rate.isEmpty() || fine.isEmpty()) {
            return ShopActionResult.fail("parking.reikocars.bad_price");
        }
        meter.setPrices(rate.getAsLong(), fine.getAsLong());
        EconomyApi eco = EconomyApi.get();
        return ShopActionResult.ok(Component.translatable("parking.reikocars.prices_set", eco.format(rate.getAsLong()), eco.format(fine.getAsLong())));
    }

    private static OptionalLong parse(String text) {
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 100) {
            return OptionalLong.empty();
        }
        return parsed;
    }
}
