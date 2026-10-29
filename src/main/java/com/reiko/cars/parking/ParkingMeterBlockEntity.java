package com.reiko.cars.parking;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A parking meter: its owner, rate and fine, its takings, and the car parked at it: when it arrived, how long it's
 * paid for, and when it was last ticketed. Once a second it looks again (see {@link #update}).
 */
public final class ParkingMeterBlockEntity extends BlockEntity {
    /** Ticks in ten minutes, the meter's unit of time. */
    public static final long TEN_MINUTES = 12_000;

    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** Cents per 10 minutes, and the fine, in cents: -1 for the server's defaults. */
    private long rate = -1;
    private long fine = -1;
    private long earned;
    private long fined;
    private int tickets;

    @Nullable
    private UUID car;
    private long arrived;
    @Nullable
    private UUID paidFor;
    private long paidUntil;
    private long lastTicket = -1;

    public ParkingMeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PARKING_METER.get(), pos, state);
    }

    // ---- owner and prices ----

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable UUID id, String name) {
        owner = id;
        ownerName = id == null ? "" : name;
        setChanged();
    }

    public void setOwner(Player player) {
        setOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long rate() {
        return rate >= 0 ? rate : Math.round(CarsConfig.get(CarsConfig.PARKING_RATE) * 100.0);
    }

    public long fine() {
        return fine >= 0 ? fine : Math.round(CarsConfig.get(CarsConfig.PARKING_FINE) * 100.0);
    }

    public void setPrices(long rate, long fine) {
        this.rate = rate;
        this.fine = fine;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public long fined() {
        return fined;
    }

    public int tickets() {
        return tickets;
    }

    void recordPayment(long amount) {
        earned += amount;
        setChanged();
    }

    void recordTicket(long collected) {
        fined += collected;
        tickets++;
        setChanged();
    }

    // ---- the car at the meter ----

    /** How many ticks of paid time {@code parked} has left at this meter, at {@code now}: 0 if none. */
    public long timeLeft(@Nullable CarEntity parked, long now) {
        if (parked == null || !parked.getUUID().equals(paidFor)) {
            return 0;
        }
        return Math.max(0, paidUntil - now);
    }

    /** Pays for {@code ticks} more for {@code parked}, from now or from when its paid time ends. */
    void addTime(CarEntity parked, long ticks, long now) {
        long from = parked.getUUID().equals(paidFor) ? Math.max(now, paidUntil) : now;
        paidFor = parked.getUUID();
        paidUntil = from + ticks;
        lastTicket = -1;
        setExpired(false);
        setChanged();
    }

    /** Server, every tick: once a second, check the car parked here. */
    void serverTick() {
        if (level != null && (level.getGameTime() + worldPosition.hashCode()) % 20 == 0) {
            update(level.getGameTime());
        }
    }

    /**
     * Looks at the car parked here at {@code now}: a car that's paid, its owner's, or has someone in it is fine; one
     * that's unpaid (or whose time has run out) for longer than the grace period gets the red flag and a ticket, and
     * another every so often while it stays.
     */
    public void update(long now) {
        CarEntity parked = MeterService.parkedAt(this);
        if (parked == null) {
            car = null;
            setExpired(false);
            return;
        }
        if (!parked.getUUID().equals(car)) {
            car = parked.getUUID();
            arrived = now;
            lastTicket = -1;
            setChanged();
        }
        boolean exempt = owner == null || parked.owner() == null || owner.equals(parked.owner());
        if (!parked.getPassengers().isEmpty()) {
            arrived = now; // someone's in it: it's waiting, not parked
            exempt = true;
        }
        if (exempt || timeLeft(parked, now) > 0) {
            setExpired(false);
            return;
        }
        long since = parked.getUUID().equals(paidFor) ? Math.max(arrived, paidUntil) : arrived;
        if (now - since < CarsConfig.get(CarsConfig.PARKING_GRACE) * 20L) {
            setExpired(false);
            return;
        }
        setExpired(true);
        if (lastTicket < 0 || now - lastTicket >= CarsConfig.get(CarsConfig.PARKING_FINE_EVERY) * 20L) {
            lastTicket = now;
            MeterService.ticket(this, parked);
            setChanged();
        }
    }

    public boolean isExpired() {
        return getBlockState().hasProperty(ParkingMeterBlock.EXPIRED) && getBlockState().getValue(ParkingMeterBlock.EXPIRED);
    }

    private void setExpired(boolean expired) {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(ParkingMeterBlock.EXPIRED) && state.getValue(ParkingMeterBlock.EXPIRED) != expired) {
            level.setBlock(worldPosition, state.setValue(ParkingMeterBlock.EXPIRED, expired), Block.UPDATE_ALL);
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Rate", rate);
        tag.putLong("Fine", fine);
        tag.putLong("Earned", earned);
        tag.putLong("Fined", fined);
        tag.putInt("Tickets", tickets);
        if (car != null) {
            tag.putUUID("Car", car);
            tag.putLong("Arrived", arrived);
        }
        if (paidFor != null) {
            tag.putUUID("PaidFor", paidFor);
            tag.putLong("PaidUntil", paidUntil);
        }
        tag.putLong("LastTicket", lastTicket);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        rate = tag.contains("Rate") ? tag.getLong("Rate") : -1;
        fine = tag.contains("Fine") ? tag.getLong("Fine") : -1;
        earned = tag.getLong("Earned");
        fined = tag.getLong("Fined");
        tickets = tag.getInt("Tickets");
        car = tag.hasUUID("Car") ? tag.getUUID("Car") : null;
        arrived = tag.getLong("Arrived");
        paidFor = tag.hasUUID("PaidFor") ? tag.getUUID("PaidFor") : null;
        paidUntil = tag.getLong("PaidUntil");
        lastTicket = tag.contains("LastTicket") ? tag.getLong("LastTicket") : -1;
    }
}
