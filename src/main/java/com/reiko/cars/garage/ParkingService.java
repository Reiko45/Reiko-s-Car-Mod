package com.reiko.cars.garage;

import com.reiko.cars.car.CarEntity;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.config.EconomyConfig;
import com.reiko.economy.core.Money;
import com.reiko.economy.shop.Payment;
import com.reiko.economy.shop.ShopActionResult;
import com.reiko.economy.shop.ShopPayments;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Parking garages. A garage door's owner can run it as one: a driver who pulls up to it (and couldn't open it anyway)
 * presses the hitch key (G) and pays for some hours or days, by card or cash plus sales tax, to the owner. The door
 * then opens for that car, coming and going, until its time's up. A car still in the garage after that pays for the
 * hours it's overstayed to get out. An hour and a day are a Minecraft hour (50 seconds) and day (20 minutes).
 */
public final class ParkingService {
    public static final int HOUR = 1000;
    public static final int DAY = 24000;
    /** What a driver can pay for: so many hours, then so many days. */
    public static final int[] HOURS = {1, 2, 6, 12};
    public static final int[] DAYS = {1, 2, 3, 7};
    public static final int OPTIONS = HOURS.length + DAYS.length;
    /** The option for paying an overstay to leave. */
    public static final int LEAVE = -1;
    /** Time to drive out once an overstay's paid, in ticks. */
    public static final int GRACE = 1200;
    /** How long an expired ticket's kept, in ticks, in case the car comes back to leave. */
    private static final long KEEP = 7L * DAY;

    private ParkingService() {
    }

    /** How long option {@code option} buys, in ticks. */
    public static int ticks(int option) {
        return option < HOURS.length ? HOURS[option] * HOUR : DAYS[option - HOURS.length] * DAY;
    }

    /** What option {@code option} costs before tax, in cents. */
    public static long price(GarageSettings settings, int option) {
        return option < HOURS.length ? HOURS[option] * settings.hourPrice() : DAYS[option - HOURS.length] * settings.dayPrice();
    }

    /** "2 hours" or "3 days". */
    public static Component optionName(int option) {
        return option < HOURS.length ? Component.translatable(HOURS[option] == 1 ? "garage.reikocars.hour" : "garage.reikocars.hours", HOURS[option])
                : Component.translatable(DAYS[option - HOURS.length] == 1 ? "garage.reikocars.day" : "garage.reikocars.days", DAYS[option - HOURS.length]);
    }

    /** Whether {@code car} has paid to park at {@code door} and its time isn't up. */
    public static boolean paidUp(CarEntity car, GarageDoorBlockEntity door) {
        GarageDoorBlockEntity.Parked ticket = door.parked().get(car.getUUID());
        return ticket != null && ticket.until() > car.level().getGameTime();
    }

    /** The hours {@code car} has overstayed at {@code door} (at least one, if it never paid), and what they cost. */
    public static long overstay(CarEntity car, GarageDoorBlockEntity door) {
        return overstayHours(car, door) * door.settings().hourPrice();
    }

    public static int overstayHours(CarEntity car, GarageDoorBlockEntity door) {
        GarageDoorBlockEntity.Parked ticket = door.parked().get(car.getUUID());
        long over = ticket == null ? HOUR : car.level().getGameTime() - ticket.until();
        return (int) Math.max(1L, (over + HOUR - 1) / HOUR);
    }

    /** The garage door just in front of {@code car}, whichever side it's on, if there's one. */
    @Nullable
    public static GarageDoorBlockEntity doorAhead(CarEntity car) {
        Level level = car.level();
        Vec3 front = car.position().add(car.forward().scale(car.carType().length / 2.0 + 1.0));
        BlockPos center = BlockPos.containing(front);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof GarageDoorBlock && state.getValue(GarageDoorBlock.PART) != GarageDoorBlock.Part.SINGLE
                    && level.getBlockEntity(pos) instanceof GarageDoorBlockEntity panel) {
                GarageDoorBlockEntity door = panel.anchorEntity();
                if (door != null) {
                    return door;
                }
            }
        }
        return null;
    }

    /** Whether {@code driver} in {@code car} would pay at {@code door}: it's a parking garage that wouldn't let them through as it is. */
    public static boolean wantsTicket(CarEntity car, Player driver, GarageDoorBlockEntity door) {
        return door.settings().parking() && !through(car, driver, door) && !paidUp(car, door);
    }

    /** Whether the door lets {@code driver} in {@code car} through as it is: theirs to open, or its owner's car. */
    public static boolean through(CarEntity car, Player driver, GarageDoorBlockEntity door) {
        return GarageDoors.canOpen(driver, door) || (door.owner() != null && door.owner().equals(car.owner()));
    }

    /**
     * The hitch key at a garage door, on the server: opens it for someone it'd let through anyway (or with time paid);
     * at a parking garage, the pay screen, to park or to leave. False if it's nothing to do with this driver.
     */
    public static boolean use(ServerPlayer driver, CarEntity car) {
        GarageDoorBlockEntity door = doorAhead(car);
        if (door == null) {
            return false;
        }
        if (through(car, driver, door) || (door.settings().parking() && paidUp(car, door))) {
            GarageDoors.toggle(door);
            return true;
        }
        if (!door.settings().parking()) {
            return false;
        }
        boolean leaving = GarageDoors.depth(car.position(), door) < 0.0;
        ParkingMenu.open(driver, door, car, leaving);
        return true;
    }

    /**
     * {@code driver} pays to park {@code option} at the garage whose door's anchor is at {@code anchor} (or, with
     * {@link #LEAVE}, for their overstay, to get out).
     *
     * @param expected the price the screen showed, before tax
     */
    public static ShopActionResult pay(ServerPlayer driver, BlockPos anchor, int option, long expected, Payment payment) {
        ServerLevel level = driver.serverLevel();
        if (!(level.getBlockEntity(anchor) instanceof GarageDoorBlockEntity door) || !door.isAnchor() || !door.settings().parking()
                || door.owner() == null) {
            return ShopActionResult.fail("garage.reikocars.gone");
        }
        if (!(driver.getVehicle() instanceof CarEntity car) || car.getControllingPassenger() != driver) {
            return ShopActionResult.fail("garage.reikocars.drive_up");
        }
        if (option != LEAVE && (option < 0 || option >= OPTIONS)) {
            return ShopActionResult.fail("garage.reikocars.gone");
        }
        long price = option == LEAVE ? overstay(car, door) : price(door.settings(), option);
        EconomyApi eco = EconomyApi.get();
        long tax = EconomyConfig.taxRates().salesTax(price);
        if (price != expected) {
            return ShopActionResult.fail("garage.reikocars.price_changed", eco.format(price + tax));
        }
        if (price + tax > Money.MAX_BALANCE) {
            return ShopActionResult.fail("garage.reikocars.bad_price");
        }
        String name = door.settings().name().isEmpty() ? door.ownerName() : door.settings().name();
        ShopPayments.Charge charge = null;
        if (price > 0) {
            charge = ShopPayments.charge(driver, payment, price, tax, door.owner(), "reikocars.parking",
                    Component.translatable("garage.reikocars.bill_line", name));
            if (!charge.success()) {
                return ShopActionResult.fail(charge.message());
            }
        }
        long now = level.getGameTime();
        GarageDoorBlockEntity.Parked old = door.parked().get(car.getUUID());
        long until = option == LEAVE ? now + GRACE : Math.max(now, old == null ? now : old.until()) + ticks(option);
        door.parked().put(car.getUUID(), new GarageDoorBlockEntity.Parked(until, car.plate(), driver.getGameProfile().getName()));
        door.recordParking(price);
        door.sync();
        GarageDoors.open(door, true);
        if (charge == null) {
            return ShopActionResult.ok(Component.translatable(option == LEAVE ? "garage.reikocars.left_free" : "garage.reikocars.parked_free",
                    optionName(Math.max(0, option))));
        }
        Component line = option == LEAVE
                ? Component.translatable("garage.reikocars.paid_leave", eco.format(price + tax))
                : Component.translatable("garage.reikocars.paid_park", optionName(option), eco.format(price + tax));
        return ShopActionResult.ok(ShopPayments.receipt(line, payment, charge));
    }

    /** Forgets tickets that ran out long ago. */
    static void forgetOld(GarageDoorBlockEntity door, long now) {
        if (door.parked().values().removeIf(ticket -> ticket.until() + KEEP < now)) {
            door.setChanged();
        }
    }

    /** The owner sets the door's name, its switches and its parking prices. */
    public static ShopActionResult save(ServerPlayer player, GarageDoorBlockEntity door, String name, int flags, String hourText, String dayText) {
        if (!door.canManage(player) || door.owner() == null) {
            return ShopActionResult.fail("garage.reikocars.not_yours", door.ownerName());
        }
        EconomyApi eco = EconomyApi.get();
        OptionalLong hour = eco.parse(hourText.isBlank() ? "0" : hourText.trim());
        OptionalLong day = eco.parse(dayText.isBlank() ? "0" : dayText.trim());
        if (hour.isEmpty() || day.isEmpty() || hour.getAsLong() < 0 || day.getAsLong() < 0
                || hour.getAsLong() > Money.MAX_BALANCE / 1000 || day.getAsLong() > Money.MAX_BALANCE / 1000) {
            return ShopActionResult.fail("garage.reikocars.bad_price");
        }
        String trimmed = name.strip();
        if (trimmed.length() > GarageSettings.MAX_NAME) {
            trimmed = trimmed.substring(0, GarageSettings.MAX_NAME);
        }
        GarageSettings old = door.settings();
        GarageSettings settings = new GarageSettings(trimmed, old.color(), (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, old.code(),
                (flags & 8) != 0, hour.getAsLong(), day.getAsLong());
        GarageDoors.applySettings(door, settings);
        return ShopActionResult.ok(Component.translatable("garage.reikocars.saved"));
    }

    /** The owner resets the door's remotes: a new code, so every remote linked to it stops working. */
    public static ShopActionResult resetRemotes(ServerPlayer player, GarageDoorBlockEntity door) {
        if (!door.canManage(player) || door.owner() == null) {
            return ShopActionResult.fail("garage.reikocars.not_yours", door.ownerName());
        }
        GarageDoors.applySettings(door, door.settings().withCode(GarageDoors.newCode()));
        return ShopActionResult.ok(Component.translatable("garage.reikocars.remotes_reset"));
    }

    /** Flags for {@link #save}: windows, opens for cars, inside is the way the panels face, parking garage. */
    public static int flags(GarageSettings settings) {
        return (settings.windows() ? 1 : 0) | (settings.autoOpen() ? 2 : 0) | (settings.inward() ? 4 : 0) | (settings.parking() ? 8 : 0);
    }
}
