package com.reiko.cars.fare;

import com.reiko.cars.car.CarEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.OptionalLong;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Paying a bus fare, and an owner setting their bus's or taxi's fares. */
public final class FareService {
    private FareService() {
    }

    /**
     * {@code rider} pays the bus fare, by card or cash, plus sales tax, to whoever's driving.
     *
     * @param expected the fare the screen showed, before tax
     */
    public static ShopActionResult payBusFare(ServerPlayer rider, CarEntity bus, long expected, Payment payment) {
        CarFares fares = bus.fares();
        if (!bus.hasPassenger(rider) || bus.getControllingPassenger() == rider) {
            return ShopActionResult.fail("fare.reikocars.not_aboard", bus.kindName());
        }
        if (fares.hasPaid(rider)) {
            return ShopActionResult.fail("fare.reikocars.already_paid");
        }
        if (!(bus.getControllingPassenger() instanceof Player driver)) {
            return ShopActionResult.fail("fare.reikocars.no_driver", bus.kindName());
        }
        long fare = fares.start();
        EconomyApi eco = EconomyApi.get();
        if (fare != expected) {
            return ShopActionResult.fail("fare.reikocars.fare_changed", eco.format(fare + EconomyConfig.taxRates().salesTax(fare)));
        }
        long tax = EconomyConfig.taxRates().salesTax(fare);
        Component description = Component.translatable("fare.reikocars.bill_line", bus.kindName(), driver.getGameProfile().getName());
        ShopPayments.Charge charge = ShopPayments.charge(rider, payment, fare, tax, driver.getUUID(), "reikocars.bus_fare", description);
        if (!charge.success()) {
            return ShopActionResult.fail(charge.message());
        }
        fares.markPaid(rider.getUUID(), fare);
        driver.displayClientMessage(Component.translatable("fare.reikocars.driver_got_fare", rider.getGameProfile().getName(), eco.format(fare)), true);
        Component line = tax > 0
                ? Component.translatable("fare.reikocars.paid_tax", eco.format(fare + tax), eco.format(tax))
                : Component.translatable("fare.reikocars.paid", eco.format(fare));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** The owner sets the fares: the bus fare, or the taxi's starting fare and rate per 100 blocks. */
    public static ShopActionResult setFares(ServerPlayer player, CarEntity car, String startText, String per100Text) {
        if (!car.canManage(player) || car.owner() == null) {
            return ShopActionResult.fail("fare.reikocars.owner_only", car.ownerName());
        }
        OptionalLong start = parse(startText);
        OptionalLong per100 = car.carType().isMetered() ? parse(per100Text) : OptionalLong.of(0);
        if (start.isEmpty() || per100.isEmpty()) {
            return ShopActionResult.fail("fare.reikocars.bad_price");
        }
        car.fares().setPrices(start.getAsLong(), per100.getAsLong());
        car.fares().tick(); // the meter shows the new rate straight away
        return ShopActionResult.ok(car.fares().priceLine());
    }

    private static OptionalLong parse(String text) {
        OptionalLong parsed = EconomyApi.get().parse(text.isBlank() ? "0" : text.trim());
        if (parsed.isEmpty() || parsed.getAsLong() < 0 || parsed.getAsLong() > Money.MAX_BALANCE / 100) {
            return OptionalLong.empty();
        }
        return parsed;
    }
}
