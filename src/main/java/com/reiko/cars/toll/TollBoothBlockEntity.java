package com.reiko.cars.toll;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A toll booth: its owner, the toll for its gates, and what it's taken. */
public final class TollBoothBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** The toll in cents; -1 for the server's default. */
    private long toll = -1;
    private long earned;
    private int cars;

    public TollBoothBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOLL_BOOTH.get(), pos, state);
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

    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long toll() {
        return toll >= 0 ? toll : Math.round(CarsConfig.get(CarsConfig.TOLL) * 100.0);
    }

    public void setToll(long cents) {
        toll = Math.max(0, cents);
        setChanged();
    }

    void recordToll(long amount) {
        earned += amount;
        cars++;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int cars() {
        return cars;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Toll", toll);
        tag.putLong("Earned", earned);
        tag.putInt("Cars", cars);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        toll = tag.contains("Toll") ? tag.getLong("Toll") : -1;
        earned = tag.getLong("Earned");
        cars = tag.getInt("Cars");
    }
}
