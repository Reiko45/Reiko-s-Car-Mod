package com.reiko.cars.wash;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Wash Bay: its owner, price and books (see {@link WashBusinessBlockEntity}), and who's paid for time on its pressure
 * washer, until when. The time's sent to clients, so they can draw the hoses.
 */
public final class WashBayBlockEntity extends WashBusinessBlockEntity {
    /** Paid time: player id to the game time it runs out. */
    private final Map<UUID, Long> time = new HashMap<>();

    public WashBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WASH_BAY.get(), pos, state);
    }

    @Override
    protected long defaultPrice() {
        return Math.round(CarsConfig.get(CarsConfig.BAY_PRICE) * 100.0);
    }

    /** Ticks of paid time {@code player} has left here (0 if none). */
    public long ticksLeft(Player player, long now) {
        Long until = time.get(player.getUUID());
        return until == null ? 0 : Math.max(0, until - now);
    }

    /** Whether {@code player} may spray here: they've time left, or it's theirs. */
    public boolean mayUse(Player player, long now) {
        return isOwner(player) || ticksLeft(player, now) > 0;
    }

    void addTime(Player player, long now, long ticks) {
        long from = Math.max(now, time.getOrDefault(player.getUUID(), 0L));
        time.put(player.getUUID(), from + ticks);
        changed();
    }

    /** Forgets time that's run out. */
    void expire(long now) {
        if (time.values().removeIf(until -> until <= now)) {
            changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- saving, and what clients get ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeTime(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        time.clear();
        for (Tag entry : tag.getList("Time", Tag.TAG_COMPOUND)) {
            CompoundTag paid = (CompoundTag) entry;
            if (paid.hasUUID("Player")) {
                time.put(paid.getUUID("Player"), paid.getLong("Until"));
            }
        }
    }

    private void writeTime(CompoundTag tag) {
        ListTag list = new ListTag();
        time.forEach((player, until) -> {
            CompoundTag paid = new CompoundTag();
            paid.putUUID("Player", player);
            paid.putLong("Until", until);
            list.add(paid);
        });
        tag.put("Time", list);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (owner() != null) {
            tag.putUUID("Owner", owner());
            tag.putString("OwnerName", ownerName());
        }
        writeTime(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
