package com.reiko.cars.garage;

import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A garage door button's owner and the door it works (its anchor). */
public final class GarageButtonBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    @Nullable
    private BlockPos door;

    public GarageButtonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GARAGE_BUTTON.get(), pos, state);
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

    public boolean canManage(Player player) {
        return owner == null || player.getUUID().equals(owner) || player.hasPermissions(2);
    }

    /** The anchor of the door it works, if it's linked to one. */
    @Nullable
    public BlockPos door() {
        return door;
    }

    public void setDoor(@Nullable BlockPos door) {
        this.door = door == null ? null : door.immutable();
        setChanged();
    }

    /** The door it works, if it's still there. */
    @Nullable
    public GarageDoorBlockEntity doorEntity() {
        if (door == null || level == null || !level.isLoaded(door)) {
            return null;
        }
        return level.getBlockEntity(door) instanceof GarageDoorBlockEntity anchor && anchor.isAnchor() ? anchor : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        if (door != null) {
            tag.put("Door", NbtUtils.writeBlockPos(door));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        door = NbtUtils.readBlockPos(tag, "Door").orElse(null);
    }
}
