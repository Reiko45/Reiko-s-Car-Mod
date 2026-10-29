package com.reiko.cars.sale;

import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A car dealership's office: its owner, what it offers for trade-ins of each kind of car (in cents; 0 for kinds it
 * isn't buying), and its books: cars sold and what they fetched, trade-ins bought and what they cost.
 */
public final class DealershipBlockEntity extends BlockEntity {
    private final long[] offers = new long[CarType.values().length];
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private long earned;
    private int sold;
    private long spent;
    private int bought;

    public DealershipBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEALERSHIP.get(), pos, state);
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

    /** The owner or an admin: they price the lot's cars, set the trade-in offers, and may break it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** What it pays for a {@code type} in perfect condition, in cents: 0 if it isn't buying them. */
    public long offer(CarType type) {
        return offers[type.ordinal()];
    }

    public long[] offers() {
        return offers.clone();
    }

    public void setOffer(CarType type, long cents) {
        offers[type.ordinal()] = Math.max(0, cents);
        setChanged();
    }

    public void recordSale(long price) {
        earned += price;
        sold++;
        setChanged();
    }

    public void recordTradeIn(long paid) {
        spent += paid;
        bought++;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int sold() {
        return sold;
    }

    public long spent() {
        return spent;
    }

    public int bought() {
        return bought;
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
            if (offers[type.ordinal()] > 0) {
                saved.putLong(type.id(), offers[type.ordinal()]); // by name, so new kinds of car don't shift them
            }
        }
        tag.put("Offers", saved);
        tag.putLong("Earned", earned);
        tag.putInt("Sold", sold);
        tag.putLong("Spent", spent);
        tag.putInt("Bought", bought);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        CompoundTag saved = tag.getCompound("Offers");
        for (CarType type : CarType.values()) {
            offers[type.ordinal()] = saved.getLong(type.id());
        }
        earned = tag.getLong("Earned");
        sold = tag.getInt("Sold");
        spent = tag.getLong("Spent");
        bought = tag.getInt("Bought");
    }
}
