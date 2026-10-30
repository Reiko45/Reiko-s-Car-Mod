package com.reiko.cars.charge;

import buildcraftreborn.api.mj.IMjConnector;
import buildcraftreborn.api.mj.IMjReadable;
import buildcraftreborn.api.mj.IMjRedstoneReceiver;
import buildcraftreborn.api.mj.MjAPI;
import buildcraftreborn.api.mj.MjCapabilityHelper;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import com.reiko.cars.registry.ModDataComponents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The working part of a charging station: the power it has stored, its price per 1,000 MJ and its owner, plus a
 * running total of what it has sold. Power comes in as BuildCraft MJ (from engines and kinesis pipes, through
 * {@link MjAPI}'s capabilities) or as other mods' energy, which BuildCraft converts as its config says; it never goes
 * out except into a car that's paid for it. Stored in micro-MJ, as BuildCraft counts it.
 */
public final class ChargingStationBlockEntity extends BlockEntity {
    /** How often, in ticks, a changing store is sent to nearby players for the display. */
    private static final int SYNC_INTERVAL = 10;

    private final Receiver receiver = new Receiver();
    private final MjCapabilityHelper capabilities = new MjCapabilityHelper(receiver);
    /** Micro-MJ stored. */
    private long stored;
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** Cents per 1,000 MJ. */
    private long price;
    private long mjSold;
    private long earned;
    private boolean needsSync;

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION.get(), pos, state);
        this.price = Math.round(CarsConfig.get(CarsConfig.DEFAULT_CHARGE_PRICE) * 100.0);
    }

    void serverTick() {
        if (needsSync && level != null && level.getGameTime() % SYNC_INTERVAL == 0) {
            needsSync = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** BuildCraft's MJ capabilities (receiver, redstone receiver, readable, connector) and an energy view, as its config allows. */
    public MjCapabilityHelper capabilities() {
        return capabilities;
    }

    /** The store as BuildCraft engines and pipes see it: power goes in, never out. */
    public IMjRedstoneReceiver receiver() {
        return receiver;
    }

    /** Micro-MJ per MJ: BuildCraft counts power in millionths. */
    public static long microPerMj() {
        return MjAPI.MJ;
    }

    /** Whole MJ stored. */
    public long storedMj() {
        return stored / microPerMj();
    }

    public long capacityMj() {
        return CarsConfig.get(CarsConfig.CHARGER_CAPACITY);
    }

    private long capacityMicro() {
        return capacityMj() * microPerMj();
    }

    /** Adds up to {@code mj} MJ to the store (an admin's top-up, or a test). Returns how much went in. */
    public long addMj(long mj) {
        long added = Math.min(mj * microPerMj(), capacityMicro() - stored);
        if (added <= 0) {
            return 0;
        }
        stored += added;
        changedQuietly();
        return added / microPerMj();
    }

    /** Takes {@code mj} MJ out of the store, for a car. */
    void drainMj(long mj) {
        stored = Math.max(0, stored - mj * microPerMj());
        changed();
    }

    public long price() {
        return price;
    }

    public void setPrice(long cents) {
        price = Math.max(0, cents);
        changed();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable Player player) {
        owner = player == null ? null : player.getUUID();
        ownerName = player == null ? "" : player.getGameProfile().getName();
        changed();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** The owner or an admin may set the price and break it. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long mjSold() {
        return mjSold;
    }

    public long earned() {
        return earned;
    }

    void recordSale(long mj, long total) {
        mjSold += mj;
        earned += total;
        changed();
    }

    /** Saved, and sent to nearby players within half a second (power arrives a little every tick). */
    private void changedQuietly() {
        setChanged();
        needsSync = true;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ---- saving and syncing ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("Stored", stored);
        tag.putLong("Price", price);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Sold", mjSold);
        tag.putLong("Earned", earned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(0, tag.getLong("Stored"));
        if (tag.contains("Price")) {
            price = tag.getLong("Price");
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        mjSold = tag.getLong("Sold");
        earned = tag.getLong("Earned");
    }

    /** Nearby players get the store, price and owner, for the display on the front. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Stored", stored);
        tag.putLong("Price", price);
        tag.putString("OwnerName", ownerName);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // A broken station keeps its power and price on the item (see the loot table's copy_components).

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        Long energy = input.get(ModDataComponents.CHARGER_ENERGY.get());
        if (energy != null) {
            stored = Math.max(0, energy);
        }
        Long savedPrice = input.get(ModDataComponents.PUMP_PRICE.get());
        if (savedPrice != null) {
            price = savedPrice;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (stored > 0) {
            components.set(ModDataComponents.CHARGER_ENERGY.get(), stored);
        }
        components.set(ModDataComponents.PUMP_PRICE.get(), price);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Stored");
        tag.remove("Price");
    }

    /** What engines, pipes and gauges talk to: takes power until full, and says how full it is. */
    private final class Receiver implements IMjRedstoneReceiver, IMjReadable {
        @Override
        public boolean canConnect(IMjConnector other) {
            return true;
        }

        @Override
        public long getPowerRequested() {
            return Math.max(0, capacityMicro() - stored);
        }

        /** Returns the power it couldn't take, as BuildCraft expects. */
        @Override
        public long receivePower(long microJoules, boolean simulate) {
            long taken = Math.max(0, Math.min(microJoules, capacityMicro() - stored));
            if (!simulate && taken > 0) {
                stored += taken;
                changedQuietly();
            }
            return microJoules - taken;
        }

        @Override
        public long getStored() {
            return stored;
        }

        @Override
        public long getCapacity() {
            return capacityMicro();
        }
    }
}
