package com.reiko.cars.rental;

import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A car rental desk: whose it is, what a day's rent is for each kind of car (in cents), and its books. */
public final class RentalDeskBlockEntity extends BlockEntity {
    private final long[] prices = new long[CarType.values().length];
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private long earned;
    private int rentals;

    public RentalDeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RENTAL_DESK.get(), pos, state);
        Arrays.fill(prices, Math.round(CarsConfig.get(CarsConfig.RENTAL_PRICE) * 100));
    }

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

    /** The owner or an admin: they set the prices and may break it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** A day's rent for a {@code type}, in cents, before tax: 0 for kinds it doesn't rent out. */
    public long price(CarType type) {
        return prices[type.ordinal()];
    }

    public long[] prices() {
        return prices.clone();
    }

    public void setPrice(CarType type, long cents) {
        prices[type.ordinal()] = Math.max(0, cents);
        setChanged();
    }

    public void recordRental(long paid) {
        rentals++;
        earned += paid;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int rentals() {
        return rentals;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        CompoundTag saved = new CompoundTag();
        for (CarType type : CarType.values()) {
            saved.putLong(type.id(), prices[type.ordinal()]); // by name, so new kinds of car don't shift them
        }
        tag.put("Prices", saved);
        tag.putLong("Earned", earned);
        tag.putInt("Rentals", rentals);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        CompoundTag saved = tag.getCompound("Prices");
        for (CarType type : CarType.values()) {
            if (saved.contains(type.id())) {
                prices[type.ordinal()] = saved.getLong(type.id());
            }
        }
        earned = tag.getLong("Earned");
        rentals = tag.getInt("Rentals");
    }
}
