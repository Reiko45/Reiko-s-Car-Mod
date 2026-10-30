package com.reiko.cars.traffic;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;

/** A speed limit sign: the limit (in mph or km/h, as its owner set it), and its speed camera's fine and tickets. */
public final class SpeedSignBlockEntity extends OwnedRoadsideBlockEntity {
    /** Speed in blocks per tick, as miles and kilometres an hour. */
    public static final double MPH_PER_BLOCK_TICK = 20.0 * 3600.0 / 1609.344;
    public static final double KMH_PER_BLOCK_TICK = 20.0 * 3.6;

    private int limit = 30;
    private boolean kmh;

    public SpeedSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEED_SIGN.get(), pos, state);
    }

    @Override
    protected long defaultFine() {
        return Math.round(CarsConfig.get(CarsConfig.SPEEDING_FINE) * 100.0);
    }

    public int limit() {
        return limit;
    }

    public boolean kmh() {
        return kmh;
    }

    public void setLimit(int limit, boolean kmh) {
        this.limit = Math.max(5, Math.min(200, limit));
        this.kmh = kmh;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); // the number on the sign
        }
    }

    /** {@code blocksPerTick} in this sign's unit. */
    public double inUnit(double blocksPerTick) {
        return blocksPerTick * (kmh ? KMH_PER_BLOCK_TICK : MPH_PER_BLOCK_TICK);
    }

    /** How far over the limit a driver may go before the camera fines them: 2 mph, or 3 km/h. */
    public int tolerance() {
        return kmh ? 3 : 2;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Limit", limit);
        tag.putBoolean("Kmh", kmh);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        limit = tag.contains("Limit") ? tag.getInt("Limit") : 30;
        kmh = tag.getBoolean("Kmh");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Limit", limit);
        tag.putBoolean("Kmh", kmh);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
