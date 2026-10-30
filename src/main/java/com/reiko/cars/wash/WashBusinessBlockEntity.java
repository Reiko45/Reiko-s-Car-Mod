package com.reiko.cars.wash;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A Car Wash or Wash Bay: its owner, its price, and its books (what it's taken, and how many washes). */
public abstract class WashBusinessBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** The price in cents; -1 for the server's default. */
    private long price = -1;
    private long earned;
    private int washes;

    protected WashBusinessBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** The server's default price, in cents. */
    protected abstract long defaultPrice();

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

    /** The owner or an admin may set the price and break it. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long price() {
        return price >= 0 ? price : defaultPrice();
    }

    public void setPrice(long cents) {
        price = Math.max(0, cents);
        setChanged();
    }

    void recordSale(long amount) {
        earned += amount;
        washes++;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int washes() {
        return washes;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Price", price);
        tag.putLong("Earned", earned);
        tag.putInt("Washes", washes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        price = tag.contains("Price") ? tag.getLong("Price") : -1;
        earned = tag.getLong("Earned");
        washes = tag.getInt("Washes");
    }
}
