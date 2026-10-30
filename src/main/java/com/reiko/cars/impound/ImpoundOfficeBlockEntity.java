package com.reiko.cars.impound;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * An impound lot's office: whose it is, what it charges to release a car (in cents), and its books: cars towed in,
 * cars released, and what the releases earned.
 */
public final class ImpoundOfficeBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private long fee = Math.round(CarsConfig.get(CarsConfig.IMPOUND_FEE) * 100);
    private long earned;
    private int impounded;
    private int released;

    public ImpoundOfficeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMPOUND_OFFICE.get(), pos, state);
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
        register();
    }

    public void setOwner(Player player) {
        setOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** The owner or an admin: they set the fee, release cars free and may break it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** What it charges to release a car, in cents (before tax). */
    public long fee() {
        return fee;
    }

    public void setFee(long cents) {
        fee = Math.max(0, cents);
        setChanged();
    }

    public void recordImpound() {
        impounded++;
        setChanged();
    }

    public void recordRelease(long paid) {
        released++;
        earned += paid;
        setChanged();
    }

    public long earned() {
        return earned;
    }

    public int impounded() {
        return impounded;
    }

    public int released() {
        return released;
    }

    /** Lists it in the registry of impound lots, so repossessed cars can be sent here. */
    private void register() {
        if (level instanceof ServerLevel server && owner != null) {
            ImpoundRegistry.get(server.getServer()).add(GlobalPos.of(server.dimension(), worldPosition), owner, ownerName);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        register();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Fee", fee);
        tag.putLong("Earned", earned);
        tag.putInt("Impounded", impounded);
        tag.putInt("Released", released);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        if (tag.contains("Fee")) {
            fee = tag.getLong("Fee");
        }
        earned = tag.getLong("Earned");
        impounded = tag.getInt("Impounded");
        released = tag.getInt("Released");
    }
}
