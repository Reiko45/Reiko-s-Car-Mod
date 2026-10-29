package com.reiko.cars.fare;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.economy.api.EconomyApi;
import com.reiko.economy.api.TransactionResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A city bus's or taxi's fares, set by its owner, and the passengers paying them, on the server.
 *
 * <p>The bus: each passenger who gets on while someone's driving is asked for the fare (a pay screen, card or cash,
 * plus sales tax, paid to the driver), and put off if they don't pay. The taxi: the first passenger to get in while
 * someone's driving hires it, and the meter runs, a starting fare plus so much per 100 blocks; when they get out the
 * fare comes out of their bank account and goes to the driver. Getting out before it's gone {@link #FREE_BLOCKS}
 * costs nothing. The vehicle's owner rides free in either.
 */
public final class CarFares {
    /** How far a taxi can go with its passenger before the fare counts, in blocks. */
    public static final double FREE_BLOCKS = 5.0;

    private final CarEntity car;
    /** The bus fare, or the taxi's starting fare, in cents; -1 for the server's default. */
    private long start = -1;
    /** The taxi's fare per 100 blocks, in cents; -1 for the server's default. */
    private long per100 = -1;
    private long earned;
    private int rides;

    // The bus: who's paid for this ride, and who's being asked.
    private final Set<UUID> paid = new HashSet<>();
    private final Set<UUID> asked = new HashSet<>();

    // The taxi's meter: who hired it, who was driving, and how far it's gone.
    @Nullable
    private UUID hirer;
    private String hirerName = "";
    @Nullable
    private UUID meterDriver;
    private String meterDriverName = "";
    private double blocks;
    @Nullable
    private Vec3 lastPos;

    public CarFares(CarEntity car) {
        this.car = car;
    }

    // ---- the prices ----

    /** The bus fare, or the taxi's starting fare, in cents. */
    public long start() {
        if (start >= 0) {
            return start;
        }
        return Math.round(CarsConfig.get(car.carType().isMetered() ? CarsConfig.TAXI_START : CarsConfig.BUS_FARE) * 100.0);
    }

    /** What the taxi's meter adds per 100 blocks, in cents (0 for the bus). */
    public long per100() {
        if (!car.carType().isMetered()) {
            return 0;
        }
        return per100 >= 0 ? per100 : Math.round(CarsConfig.get(CarsConfig.TAXI_PER_100) * 100.0);
    }

    public void setPrices(long start, long per100) {
        this.start = start;
        this.per100 = car.carType().isMetered() ? per100 : 0;
    }

    public long earned() {
        return earned;
    }

    public int rides() {
        return rides;
    }

    /** "$2.00 a ride", or "$3.00 + $1.00 per 100 blocks", or "Free". */
    public Component priceLine() {
        EconomyApi eco = EconomyApi.get();
        if (start() <= 0 && per100() <= 0) {
            return Component.translatable("fare.reikocars.free_rides", car.kindName());
        }
        if (car.carType().isMetered()) {
            return Component.translatable("fare.reikocars.taxi_prices", car.kindName(), eco.format(start()), eco.format(per100()));
        }
        return Component.translatable("fare.reikocars.bus_price", car.kindName(), eco.format(start()));
    }

    /** Riders who never pay: the vehicle's owner. */
    private boolean rideFree(Player player) {
        return car.isOwner(player);
    }

    // ---- riding ----

    /** Each server tick (while the vehicle isn't itself being carried). */
    public void tick() {
        if (!car.carType().takesFares()) {
            return;
        }
        Player driver = car.getControllingPassenger() instanceof Player p ? p : null;
        List<Player> riders = new ArrayList<>();
        for (Entity passenger : car.getPassengers()) {
            if (passenger instanceof Player player && player != driver) {
                riders.add(player);
            }
        }
        if (car.carType().isMetered()) {
            meterTick(driver, riders);
        } else if (driver != null && start() > 0) {
            for (Player rider : riders) {
                UUID id = rider.getUUID();
                if (rider instanceof ServerPlayer serverRider && !rideFree(rider) && !paid.contains(id) && !asked.contains(id)) {
                    asked.add(id);
                    FareMenu.open(serverRider, car, driver);
                }
            }
        }
    }

    private void meterTick(@Nullable Player driver, List<Player> riders) {
        Vec3 pos = car.position();
        double moved = lastPos == null ? 0.0 : Math.sqrt(Mth.square(pos.x - lastPos.x) + Mth.square(pos.z - lastPos.z));
        lastPos = pos;
        if (moved > 8.0) {
            moved = 0.0; // teleported, not driven
        }
        if (hirer != null && driver != null && hirer.equals(driver.getUUID())) {
            settle(); // the passenger took the wheel: the ride's over
        }
        if (hirer == null && driver != null && (start() > 0 || per100() > 0)) {
            for (Player rider : riders) {
                if (!rideFree(rider)) {
                    hire(rider, driver);
                    break;
                }
            }
        }
        if (hirer != null && driver != null) {
            blocks += moved;
        }
        car.setMeter(hirer == null ? -1L : fare());
    }

    private void hire(Player rider, Player driver) {
        hirer = rider.getUUID();
        hirerName = rider.getGameProfile().getName();
        meterDriver = driver.getUUID();
        meterDriverName = driver.getGameProfile().getName();
        blocks = 0.0;
        EconomyApi eco = EconomyApi.get();
        rider.sendSystemMessage(Component.translatable("fare.reikocars.meter_on", eco.format(start()), eco.format(per100()), (int) FREE_BLOCKS));
    }

    /** The meter's fare so far, in cents. */
    public long fare() {
        return start() + Math.round(per100() * blocks / 100.0);
    }

    public double meterBlocks() {
        return blocks;
    }

    @Nullable
    public UUID hirer() {
        return hirer;
    }

    /** Someone got out (or off), on the server. */
    public void riderLeft(Entity passenger) {
        UUID id = passenger.getUUID();
        paid.remove(id);
        asked.remove(id);
        if (id.equals(hirer)) {
            settle();
        }
    }

    /** The bus: a rider paid. */
    void markPaid(UUID rider, long amount) {
        paid.add(rider);
        asked.remove(rider);
        earned += amount;
        rides++;
    }

    /** The bus: the pay screen closed. Anyone who didn't pay gets off. */
    void screenClosed(ServerPlayer rider) {
        asked.remove(rider.getUUID());
        if (!paid.contains(rider.getUUID()) && car.hasPassenger(rider) && car.getControllingPassenger() != rider) {
            rider.stopRiding();
            rider.displayClientMessage(Component.translatable("fare.reikocars.put_off", car.kindName()), true);
        }
    }

    boolean hasPaid(Player rider) {
        return paid.contains(rider.getUUID());
    }

    /** The taxi: the hirer's ride is over. The fare comes out of their bank account, to the driver. */
    private void settle() {
        UUID who = hirer;
        UUID to = meterDriver;
        long fare = fare();
        double distance = blocks;
        hirer = null;
        meterDriver = null;
        blocks = 0.0;
        car.setMeter(-1L);
        MinecraftServer server = car.level().getServer();
        if (who == null || to == null || server == null) {
            return;
        }
        ServerPlayer rider = server.getPlayerList().getPlayer(who);
        ServerPlayer driver = server.getPlayerList().getPlayer(to);
        if (distance < FREE_BLOCKS) {
            if (rider != null) {
                rider.displayClientMessage(Component.translatable("fare.reikocars.no_charge"), true);
            }
            return;
        }
        EconomyApi eco = EconomyApi.get();
        long pay = Math.min(fare, Math.max(0L, eco.getBalance(server, who)));
        if (pay > 0) {
            TransactionResult result = eco.transfer(server, who, to, pay, "reikocars.taxi_fare");
            if (!result.isSuccess()) {
                pay = 0;
            }
        }
        if (pay > 0) {
            earned += pay;
            rides++;
        }
        int dist = (int) Math.round(distance);
        if (rider != null) {
            rider.sendSystemMessage(pay >= fare
                    ? Component.translatable("fare.reikocars.taxi_paid", eco.format(pay), dist, meterDriverName)
                    : Component.translatable("fare.reikocars.taxi_short", eco.format(pay), eco.format(fare), dist));
        }
        if (driver != null) {
            driver.sendSystemMessage(pay >= fare
                    ? Component.translatable("fare.reikocars.driver_paid", hirerName, eco.format(pay), dist)
                    : Component.translatable("fare.reikocars.driver_short", hirerName, eco.format(pay), eco.format(fare)));
        }
    }

    // ---- saving ----

    public void save(CompoundTag tag) {
        if (!car.carType().takesFares()) {
            return;
        }
        CompoundTag fares = new CompoundTag();
        fares.putLong("Start", start);
        fares.putLong("Per100", per100);
        fares.putLong("Earned", earned);
        fares.putInt("Rides", rides);
        tag.put("Fares", fares);
    }

    public void load(CompoundTag tag) {
        if (!tag.contains("Fares")) {
            return;
        }
        CompoundTag fares = tag.getCompound("Fares");
        start = fares.getLong("Start");
        per100 = fares.getLong("Per100");
        earned = fares.getLong("Earned");
        rides = fares.getInt("Rides");
    }
}
