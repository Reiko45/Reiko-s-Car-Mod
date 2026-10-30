package com.reiko.cars.racing;

import com.reiko.cars.racing.FlagRegistry;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** A race flag: whose it is and its number on a race track (0 for none). */
public final class RaceFlagBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private int number;

    public RaceFlagBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RACE_FLAG.get(), pos, state);
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
        changed();
    }

    public boolean canManage(Player player) {
        return owner == null || player.getUUID().equals(owner) || player.hasPermissions(2);
    }

    public int number() {
        return number;
    }

    public void setNumber(int number) {
        this.number = Math.max(0, Math.min(RaceFlagBlock.MAX_NUMBER, number));
        changed();
    }

    private void changed() {
        setChanged();
        register();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void register() {
        if (level instanceof ServerLevel server && owner != null) {
            FlagRegistry.get(server.getServer()).set(GlobalPos.of(server.dimension(), worldPosition), owner, number);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (number > 0) {
            register();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putInt("Number", number);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        number = tag.getInt("Number");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Number", number);
        tag.putString("OwnerName", ownerName);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
