package com.reiko.cars.dmv;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.license.DriverLicense;
import com.reiko.cars.license.LicenseService;
import com.reiko.cars.plate.PlateRegistry;
import com.reiko.cars.plate.Plates;
import com.reiko.cars.rental.RentalService;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * What a DMV does: the driving test, replacement license cards and custom plates, each paid by card or cash (plus
 * sales tax) to its owner. Its owner pays nothing at their own DMV.
 */
public final class DmvService {
    private DmvService() {
    }

    public static double reach() {
        return CarsConfig.get(CarsConfig.DMV_REACH);
    }

    /** The DMV's test route: its owner's numbered cones within {@code licenses.routeRadius}, in order. */
    public static List<BlockPos> route(DmvOfficeBlockEntity dmv) {
        if (!(dmv.getLevel() instanceof ServerLevel level) || dmv.owner() == null) {
            return List.of();
        }
        return ConeRegistry.get(level.getServer()).route(level.dimension(), dmv.getBlockPos(), CarsConfig.get(CarsConfig.ROUTE_RADIUS), dmv.owner());
    }

    /** {@code player}'s own cars parked within reach of the DMV, nearest first: the ones they can buy plates for. */
    public static List<CarEntity> cars(DmvOfficeBlockEntity dmv, Player player) {
        if (dmv.getLevel() == null) {
            return List.of();
        }
        BlockPos pos = dmv.getBlockPos();
        double reach = reach();
        return dmv.getLevel().getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(reach, 6.0, reach),
                        car -> car.isAlive() && car.isOwner(player) && !car.isPassenger() && car.position().distanceTo(pos.getCenter()) <= reach + 0.5)
                .stream().sorted(Comparator.comparingDouble(car -> car.distanceToSqr(pos.getCenter()))).toList();
    }

    /** What {@code player} pays for {@code which} here, before tax: nothing at their own DMV. */
    public static long price(DmvOfficeBlockEntity dmv, Player player, int which) {
        return dmv.isOwner(player) ? 0 : dmv.fee(which);
    }

    /** Charges {@code price} (plus tax) for {@code line}, or nothing at all if it's free. Null and a reason on failure. */
    @Nullable
    private static ShopPayments.Charge charge(ServerPlayer player, DmvOfficeBlockEntity dmv, Payment payment, long price, Component line,
                                              Component[] why) {
        if (price <= 0) {
            return null;
        }
        long tax = EconomyConfig.taxRates().salesTax(price);
        if (price + tax > Money.MAX_BALANCE) {
            why[0] = Component.translatable("dmv.reikocars.too_expensive");
            return null;
        }
        ShopPayments.Charge charge = ShopPayments.charge(player, payment, price, tax, dmv.owner(), "reikocars.dmv", line);
        if (!charge.success()) {
            why[0] = charge.message();
        }
        return charge;
    }

    private static ShopActionResult done(Component message, Payment payment, @Nullable ShopPayments.Charge charge) {
        return ShopActionResult.ok(charge == null ? message : ShopPayments.receipt(message, payment, charge));
    }

    /** {@code player} pays for the driving test and starts it. */
    public static ShopActionResult takeTest(ServerPlayer player, DmvOfficeBlockEntity dmv, long expected, Payment payment) {
        if (dmv.owner() == null) {
            return ShopActionResult.fail("dmv.reikocars.no_owner");
        }
        switch (LicenseService.status(player.server, player.getUUID())) {
            case VALID -> {
                return ShopActionResult.fail("dmv.reikocars.have_license");
            }
            case SUSPENDED -> {
                DriverLicense license = LicenseService.of(player.server, player.getUUID());
                return ShopActionResult.fail("dmv.reikocars.wait_suspension",
                        RentalService.timeLeft(license.suspendedUntil() - LicenseService.now(player.server)));
            }
            default -> {
            }
        }
        if (DrivingTests.taking(player)) {
            return ShopActionResult.fail("dmv.reikocars.on_test");
        }
        List<BlockPos> route = route(dmv);
        if (route.isEmpty()) {
            return ShopActionResult.fail("dmv.reikocars.no_route", dmv.ownerName());
        }
        long price = price(dmv, player, DmvOfficeBlockEntity.TEST_FEE);
        if (price != expected) {
            return ShopActionResult.fail("dmv.reikocars.price_changed", EconomyApi.get().format(price));
        }
        Component[] why = new Component[1];
        ShopPayments.Charge charge = charge(player, dmv, payment, price, Component.translatable("dmv.reikocars.bill_test", dmv.ownerName()), why);
        if (why[0] != null) {
            return ShopActionResult.fail(why[0]);
        }
        dmv.recordTest(price);
        DrivingTests.start(player, dmv, route);
        return done(Component.translatable("dmv.reikocars.test_paid"), payment, charge);
    }

    /** {@code player} pays for a new card for their license (a lost one, or for a friend to see). */
    public static ShopActionResult replace(ServerPlayer player, DmvOfficeBlockEntity dmv, long expected, Payment payment) {
        if (dmv.owner() == null) {
            return ShopActionResult.fail("dmv.reikocars.no_owner");
        }
        DriverLicense license = LicenseService.of(player.server, player.getUUID());
        if (license == null) {
            return ShopActionResult.fail("dmv.reikocars.no_license");
        }
        long price = price(dmv, player, DmvOfficeBlockEntity.REPLACE_FEE);
        if (price != expected) {
            return ShopActionResult.fail("dmv.reikocars.price_changed", EconomyApi.get().format(price));
        }
        Component[] why = new Component[1];
        ShopPayments.Charge charge = charge(player, dmv, payment, price, Component.translatable("dmv.reikocars.bill_card", dmv.ownerName()), why);
        if (why[0] != null) {
            return ShopActionResult.fail(why[0]);
        }
        dmv.recordEarnings(price);
        LicenseService.give(player, license);
        return done(Component.translatable("dmv.reikocars.card_printed", license.number()), payment, charge);
    }

    /** {@code player} buys {@code car} the custom plate {@code text}. */
    public static ShopActionResult buyPlate(ServerPlayer player, DmvOfficeBlockEntity dmv, @Nullable CarEntity car, String text, long expected,
                                            Payment payment) {
        if (dmv.owner() == null) {
            return ShopActionResult.fail("dmv.reikocars.no_owner");
        }
        if (car == null || !cars(dmv, player).contains(car)) {
            return ShopActionResult.fail("dmv.reikocars.no_car", (int) reach());
        }
        String plate = Plates.clean(text);
        if (plate == null) {
            return ShopActionResult.fail("dmv.reikocars.bad_plate", Plates.MAX_LENGTH);
        }
        if (plate.equals(car.plate())) {
            return ShopActionResult.fail("dmv.reikocars.same_plate", plate);
        }
        PlateRegistry plates = PlateRegistry.get(player.server);
        if (plates.isTaken(plate)) {
            return ShopActionResult.fail("dmv.reikocars.plate_taken", plate);
        }
        long price = price(dmv, player, DmvOfficeBlockEntity.PLATE_FEE);
        if (price != expected) {
            return ShopActionResult.fail("dmv.reikocars.price_changed", EconomyApi.get().format(price));
        }
        Component[] why = new Component[1];
        ShopPayments.Charge charge = charge(player, dmv, payment, price,
                Component.translatable("dmv.reikocars.bill_plate", plate, car.kindName(), dmv.ownerName()), why);
        if (why[0] != null) {
            return ShopActionResult.fail(why[0]);
        }
        plates.take(plate);
        if (!car.plate().isEmpty()) {
            plates.release(car.plate());
        }
        car.setPlate(plate);
        dmv.recordPlate(price);
        car.level().playSound(null, car.getX(), car.getY(), car.getZ(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.PLAYERS, 1.0F, 1.0F);
        return done(Component.translatable("dmv.reikocars.plate_bought", car.kindName(), plate), payment, charge);
    }

    /** The owner sets one of the DMV's fees. */
    public static ShopActionResult setFee(ServerPlayer player, DmvOfficeBlockEntity dmv, int which, String text) {
        if (!dmv.canManage(player) || dmv.owner() == null) {
            return ShopActionResult.fail("dmv.reikocars.owner_only", dmv.ownerName());
        }
        if (which < 0 || which > DmvOfficeBlockEntity.PLATE_FEE) {
            return ShopActionResult.fail("dmv.reikocars.bad_price");
        }
        OptionalLong price = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (price.isEmpty() || price.getAsLong() < 0 || price.getAsLong() > Money.MAX_BALANCE / 2) {
            return ShopActionResult.fail("dmv.reikocars.bad_price");
        }
        dmv.setFee(which, price.getAsLong());
        return ShopActionResult.ok(Component.translatable("dmv.reikocars.fee_set." + which, EconomyApi.get().format(price.getAsLong())));
    }
}
