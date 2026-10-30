package com.reiko.cars.traffic;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A traffic light's or speed sign's owner, its camera's fine, and its books. While it's loaded, it's on its level's
 * list of cameras (see {@link TrafficCameras}).
 */
public abstract class OwnedRoadsideBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** The camera's fine in cents; -1 for the server's default, 0 for no camera. */
    private long fine = -1;
    private int tickets;
    private long collected;

    protected OwnedRoadsideBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** The server's default fine, in cents. */
    protected abstract long defaultFine();

    public long fine() {
        return fine >= 0 ? fine : defaultFine();
    }

    public void setFine(long cents) {
        fine = Math.max(0, cents);
        setChanged();
    }

    void recordTicket(long paid) {
        tickets++;
        collected += paid;
        setChanged();
    }

    public int tickets() {
        return tickets;
    }

    public long collected() {
        return collected;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        TrafficCameras.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        TrafficCameras.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        TrafficCameras.remove(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Fine", fine);
        tag.putInt("Tickets", tickets);
        tag.putLong("Collected", collected);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        fine = tag.contains("Fine") ? tag.getLong("Fine") : -1;
        tickets = tag.getInt("Tickets");
        collected = tag.getLong("Collected");
    }
}
