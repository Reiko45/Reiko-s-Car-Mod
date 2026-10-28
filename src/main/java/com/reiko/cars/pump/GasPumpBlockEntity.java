package com.reiko.cars.pump;

import com.reiko.cars.car.CarFuel;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

/**
 * The working part of a gas pump: its fuel tank, price per bucket and owner, plus a running total of what it has
 * sold. The owner gets paid for every sale, straight into their bank account.
 */
public final class GasPumpBlockEntity extends BlockEntity {
    /** How often, in ticks, a changing tank is sent to nearby players for the price display. */
    private static final int SYNC_INTERVAL = 10;

    private final FluidTank tank;
    /** What pipes see: fuel goes in, nothing comes out, so the pump can only be emptied by selling. */
    private final IFluidHandler pipeInput;
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** Cents per bucket. */
    private long price;
    private long millibucketsSold;
    private long earned;
    private boolean needsSync;

    public GasPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_PUMP.get(), pos, state);
        this.price = Math.round(CarsConfig.get(CarsConfig.DEFAULT_PUMP_PRICE) * 100.0);
        this.tank = new FluidTank(CarsConfig.get(CarsConfig.PUMP_CAPACITY) * 1000, CarFuel::isFuel) {
            @Override
            protected void onContentsChanged() {
                setChanged();
                needsSync = true;
            }
        };
        this.pipeInput = new InsertOnly(tank);
    }

    void serverTick() {
        if (needsSync && level != null && level.getGameTime() % SYNC_INTERVAL == 0) {
            needsSync = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The full tank, for the owner's buckets and for selling. */
    public FluidTank tank() {
        return tank;
    }

    /** The tank as pipes see it: fill only. */
    public IFluidHandler pipeInput() {
        return pipeInput;
    }

    public FluidStack fuel() {
        return tank.getFluid();
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

    /** The owner or an admin may set the price, fill the pump by hand and break it. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long millibucketsSold() {
        return millibucketsSold;
    }

    public long earned() {
        return earned;
    }

    void recordSale(int millibuckets, long total) {
        millibucketsSold += millibuckets;
        earned += total;
        changed();
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
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("Price", price);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Sold", millibucketsSold);
        tag.putLong("Earned", earned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        if (tag.contains("Price")) {
            price = tag.getLong("Price");
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        millibucketsSold = tag.getLong("Sold");
        earned = tag.getLong("Earned");
    }

    /** Nearby players get the fuel, price and owner, for the display on the front of the pump. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putLong("Price", price);
        tag.putString("OwnerName", ownerName);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // A broken pump keeps its fuel and price on the item (see the loot table's copy_components).

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        SimpleFluidContent fuel = input.get(ModDataComponents.PUMP_FUEL.get());
        if (fuel != null) {
            tank.setFluid(fuel.copy());
        }
        Long savedPrice = input.get(ModDataComponents.PUMP_PRICE.get());
        if (savedPrice != null) {
            price = savedPrice;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!tank.isEmpty()) {
            components.set(ModDataComponents.PUMP_FUEL.get(), SimpleFluidContent.copyOf(tank.getFluid()));
        }
        components.set(ModDataComponents.PUMP_PRICE.get(), price);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("Tank");
        tag.remove("Price");
    }

    /** Lets fuel in but never out. */
    private record InsertOnly(FluidTank tank) implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
            return tank.getFluidInTank(index);
        }

        @Override
        public int getTankCapacity(int index) {
            return tank.getTankCapacity(index);
        }

        @Override
        public boolean isFluidValid(int index, FluidStack stack) {
            return tank.isFluidValid(index, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
