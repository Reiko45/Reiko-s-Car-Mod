package com.reiko.cars.impound;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.finance.LoanService;
import com.reiko.cars.job.CarJobs;
import com.reiko.cars.parking.MeterService;
import com.reiko.cars.parking.ParkingMeterBlockEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Impound lots. A car parked at an expired parking meter can be towed by anyone with a tow truck, locked or not; put
 * down on an impound lot (within {@code impound.lotRadius} blocks of its office), it's impounded there, and the tow
 * pays the Mechanic job. A repossessed car (see {@link LoanService}) is sent to the impound lot nearest its dealership.
 * Held, a car can't be got into, driven or picked up by its owner; they get it back by paying the office's release fee
 * (plus sales tax), and for a repossessed car the missed loan payments too, by card or cash. The lot's owner can let a
 * towed car go free, and can drive the cars about their lot.
 */
public final class ImpoundService {
    /** How far beyond its lot a car sent to an impound lot may have been put down, finding room. */
    private static final double SPILL = 10.0;

    private ImpoundService() {
    }

    public static double lotRadius() {
        return CarsConfig.get(CarsConfig.IMPOUND_LOT_RADIUS);
    }

    private static double flat(Vec3 a, Vec3 b) {
        return Math.sqrt((a.x - b.x) * (a.x - b.x) + (a.z - b.z) * (a.z - b.z));
    }

    /** Whether {@code car} is parked at a parking meter whose time's run out: then a tow truck may take it to an impound lot. */
    public static boolean badlyParked(CarEntity car) {
        if (car.owner() == null || car.isImpounded() || car.isPassenger()) {
            return false;
        }
        ParkingMeterBlockEntity meter = MeterService.nearestMeter(car);
        return meter != null && meter.isExpired() && MeterService.parkedAt(meter) == car && !car.owner().equals(meter.owner());
    }

    /** The impound office whose lot {@code pos} is on (the nearest, if lots overlap), or null. */
    @Nullable
    public static ImpoundOfficeBlockEntity lotAt(ServerLevel level, Vec3 pos) {
        double reach = lotRadius();
        ImpoundOfficeBlockEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ImpoundRegistry.Lot lot : ImpoundRegistry.get(level.getServer()).all()) {
            if (lot.pos().dimension() != level.dimension()) {
                continue;
            }
            BlockPos at = lot.pos().pos();
            double distance = flat(pos, at.getCenter());
            if (distance <= reach && distance < bestDistance && level.isLoaded(at)
                    && level.getBlockEntity(at) instanceof ImpoundOfficeBlockEntity office && office.owner() != null) {
                best = office;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Holds {@code car} at {@code lot}. */
    public static void impound(CarEntity car, ImpoundOfficeBlockEntity lot, String reason, long arrears, @Nullable UUID loan) {
        car.setImpound(new Impound(lot.getBlockPos(), lot.owner(), lot.ownerName(), car.level().getGameTime(), reason, arrears,
                Optional.ofNullable(loan)));
        lot.recordImpound();
    }

    /** A tow truck put {@code car} down: if it was towed from an expired meter and it's now on an impound lot, it's impounded. */
    public static void towedIn(@Nullable ServerPlayer driver, CarEntity car) {
        boolean badly = car.towedBadlyParked();
        car.setTowedBadlyParked(false);
        if (!badly || car.isImpounded() || !(car.level() instanceof ServerLevel level)) {
            return;
        }
        ImpoundOfficeBlockEntity lot = lotAt(level, car.position());
        if (lot == null) {
            return;
        }
        impound(car, lot, Impound.PARKING, 0, null);
        EconomyApi eco = EconomyApi.get();
        if (driver != null) {
            driver.displayClientMessage(Component.translatable("impound.reikocars.towed_in", car.kindName(), lot.ownerName()), true);
            if (!driver.getUUID().equals(car.owner()) && !driver.getUUID().equals(lot.owner())) {
                CarJobs.work(driver, CarJobs.IMPOUND_TOW, 1);
            }
        }
        BlockPos at = lot.getBlockPos();
        ServerPlayer owner = car.owner() == null ? null : level.getServer().getPlayerList().getPlayer(car.owner());
        if (owner != null) {
            owner.sendSystemMessage(Component.translatable("impound.reikocars.your_car_towed", car.kindName(), at.getX(), at.getY(), at.getZ(),
                    eco.format(lot.fee())));
        }
        level.playSound(null, at, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F);
    }

    /** The cars held at {@code lot}, nearest first. */
    public static List<CarEntity> heldAt(ImpoundOfficeBlockEntity lot) {
        if (lot.getLevel() == null) {
            return List.of();
        }
        BlockPos pos = lot.getBlockPos();
        Vec3 center = pos.getCenter();
        return lot.getLevel().getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(lotRadius() + SPILL, 8.0, lotRadius() + SPILL),
                        car -> car.isAlive() && car.impound() != null && car.impound().lot().equals(pos))
                .stream().sorted(Comparator.comparingDouble(car -> flat(car.position(), center))).toList();
    }

    /** {@code player}'s own cars held at {@code lot}. */
    public static List<CarEntity> heldFor(ImpoundOfficeBlockEntity lot, ServerPlayer player) {
        return heldAt(lot).stream().filter(car -> car.isOwner(player)).toList();
    }

    /** What it takes to get {@code car} back from {@code lot}, before tax: the release fee, and any missed loan payments. */
    public static long cost(ImpoundOfficeBlockEntity lot, CarEntity car) {
        Impound held = car.impound();
        return lot.fee() + (held == null ? 0 : held.arrears());
    }

    /** Sales tax on getting {@code car} back: on the fee, not the loan payments. */
    public static long tax(ImpoundOfficeBlockEntity lot) {
        return lot.fee() > 0 ? EconomyConfig.taxRates().salesTax(lot.fee()) : 0;
    }

    /**
     * {@code payer} pays to get their {@code car} back from {@code lot}, by card or cash: the fee (plus tax) to the lot's
     * owner, and for a repossessed car the missed payments on to its lender, which puts the loan back in good standing.
     *
     * @param expected the cost the screen showed, before tax
     */
    public static ShopActionResult release(ServerPlayer payer, ImpoundOfficeBlockEntity lot, CarEntity car, long expected, Payment payment) {
        Impound held = car.impound();
        if (held == null || !held.lot().equals(lot.getBlockPos()) || !car.isAlive()) {
            return ShopActionResult.fail("impound.reikocars.not_here");
        }
        if (!car.isOwner(payer)) {
            return ShopActionResult.fail("impound.reikocars.not_your_car");
        }
        EconomyApi eco = EconomyApi.get();
        long cost = cost(lot, car);
        if (cost != expected) {
            return ShopActionResult.fail("impound.reikocars.price_changed", eco.format(cost + tax(lot)));
        }
        long tax = tax(lot);
        if (cost + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("impound.reikocars.too_expensive");
        }
        ShopPayments.Charge charge = null;
        if (cost > 0) {
            Component line = Component.translatable("impound.reikocars.bill_line", car.kindName(), lot.ownerName());
            charge = ShopPayments.charge(payer, payment, cost, tax, lot.owner(), "reikocars.impound_release", line);
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        if (held.loan().isPresent()) {
            LoanService.arrearsPaid(payer.server, held.loan().get(), lot.owner(), held.arrears());
        }
        lot.recordRelease(lot.fee());
        car.setImpound(null);
        payer.level().playSound(null, lot.getBlockPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8F, 1.0F);
        ServerPlayer owner = lot.owner() == null ? null : payer.server.getPlayerList().getPlayer(lot.owner());
        if (owner != null && owner != payer) {
            owner.sendSystemMessage(Component.translatable("impound.reikocars.owner_released", payer.getGameProfile().getName(), car.kindName(),
                    eco.format(lot.fee())));
        }
        Component line = Component.translatable("impound.reikocars.released", car.kindName(), eco.format(cost + tax));
        return ShopActionResult.ok(charge == null ? line : ShopPayments.receipt(line, payment, charge));
    }

    /** The lot's owner lets a towed-in car go without charge (not a repossessed one: its lender's owed). */
    public static ShopActionResult releaseFree(ServerPlayer manager, ImpoundOfficeBlockEntity lot, CarEntity car) {
        Impound held = car.impound();
        if (!lot.canManage(manager) || lot.owner() == null) {
            return ShopActionResult.fail("impound.reikocars.owner_only", lot.ownerName());
        }
        if (held == null || !held.lot().equals(lot.getBlockPos()) || !car.isAlive()) {
            return ShopActionResult.fail("impound.reikocars.not_here");
        }
        if (held.repossessed()) {
            return ShopActionResult.fail("impound.reikocars.repossessed_not_free");
        }
        car.setImpound(null);
        lot.recordRelease(0);
        ServerPlayer owner = car.owner() == null ? null : manager.server.getPlayerList().getPlayer(car.owner());
        if (owner != null && owner != manager) {
            owner.sendSystemMessage(Component.translatable("impound.reikocars.let_go", car.kindName(), lot.ownerName()));
        }
        return ShopActionResult.ok(Component.translatable("impound.reikocars.released_free", car.kindName()));
    }

    /** The lot's owner sets the release fee. */
    public static ShopActionResult setFee(ServerPlayer player, ImpoundOfficeBlockEntity lot, String text) {
        if (!lot.canManage(player) || lot.owner() == null) {
            return ShopActionResult.fail("impound.reikocars.owner_only", lot.ownerName());
        }
        OptionalLong fee = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (fee.isEmpty() || fee.getAsLong() < 0 || fee.getAsLong() > Money.MAX_BALANCE / 100) {
            return ShopActionResult.fail("impound.reikocars.bad_price");
        }
        lot.setFee(fee.getAsLong());
        return ShopActionResult.ok(Component.translatable("impound.reikocars.fee_set", EconomyApi.get().format(fee.getAsLong())));
    }

    /** The office at {@code pos} was broken: the cars it held are free to go. */
    static void officeGone(ServerLevel level, BlockPos pos) {
        for (CarEntity car : level.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(lotRadius() + SPILL, 8.0, lotRadius() + SPILL),
                car -> car.impound() != null && car.impound().lot().equals(pos))) {
            car.setImpound(null);
        }
    }

    /** On the server, every few seconds, for a held car: if its lot's office is gone, it's free. */
    public static void carTick(CarEntity car) {
        Impound held = car.impound();
        if (held != null && car.level().isLoaded(held.lot()) && !(car.level().getBlockEntity(held.lot()) instanceof ImpoundOfficeBlockEntity)) {
            car.setImpound(null);
        }
    }
}
