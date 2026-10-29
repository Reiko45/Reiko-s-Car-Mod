package com.reiko.cars.toll;

import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The pole of a toll gate: whose it is, whether its owner's holding it open, and (on the client) how far the arm's
 * swung up, which the renderer draws.
 */
public final class TollGateBlockEntity extends BlockEntity {
    @Nullable
    private UUID owner;
    private String ownerName = "";
    private boolean held;
    // client: the arm's angle, 0 down to 1 up, this tick and last
    private float lift;
    private float liftO;

    public TollGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOLL_GATE.get(), pos, state);
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

    public void setOwner(@Nullable UUID id, String name) {
        owner = id;
        ownerName = id == null ? "" : name;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** Held open by its owner: it doesn't close by itself. */
    public boolean isHeld() {
        return held;
    }

    public void setHeld(boolean held) {
        this.held = held;
        setChanged();
    }

    /** Client, every tick: swing the arm toward up or down. */
    void clientTick() {
        liftO = lift;
        boolean open = getBlockState().getValue(TollGateBlock.OPEN);
        lift = open ? Math.min(1.0F, lift + 0.08F) : Math.max(0.0F, lift - 0.08F);
    }

    /** How far the arm's swung up, 0 to 1. */
    public float lift(float partialTick) {
        return liftO + (lift - liftO) * partialTick;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putBoolean("Held", held);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        held = tag.getBoolean("Held");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
