package com.reiko.cars.tuning;

import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModBlockEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The working part of a Tuning Bench: its owner, the tuning kits it has in stock for customers, the price of each
 * kind of kit fitted, and what it has earned. The stock only takes tuning kits; hoppers and pipes can put them in, but
 * nothing takes them out but jobs and the owner.
 */
public final class TuningBenchBlockEntity extends BlockEntity {
    public static final int STOCK_SLOTS = 18;

    private final ItemStackHandler stock = new ItemStackHandler(STOCK_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return TuningService.kitOf(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final IItemHandler pipeInput = new InsertOnly(stock);
    @Nullable
    private UUID owner;
    private String ownerName = "";
    /** Cents per kit fitted, by {@link TuningKit}. */
    private final long[] prices = new long[TuningKit.values().length];
    private long earned;
    private int jobs;

    public TuningBenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TUNING_BENCH.get(), pos, state);
        java.util.Arrays.fill(prices, Math.round(CarsConfig.get(CarsConfig.DEFAULT_KIT_PRICE) * 100.0));
    }

    /** The whole stock, for the owner's slots. */
    public ItemStackHandler stock() {
        return stock;
    }

    /** The stock as hoppers and pipes see it: parts in only. */
    public IItemHandler pipeInput() {
        return pipeInput;
    }

    /** How many of {@code kit} are in stock. */
    public int count(TuningKit kit) {
        int count = 0;
        for (int slot = 0; slot < stock.getSlots(); slot++) {
            ItemStack stack = stock.getStackInSlot(slot);
            if (TuningService.kitOf(stack) == kit) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /** Takes up to {@code count} of {@code kit} from stock and returns how many it took. */
    public int take(TuningKit kit, int count) {
        int taken = 0;
        for (int slot = 0; slot < stock.getSlots() && taken < count; slot++) {
            if (TuningService.kitOf(stock.getStackInSlot(slot)) == kit) {
                taken += stock.extractItem(slot, count - taken, false).getCount();
            }
        }
        return taken;
    }

    public long price(TuningKit kit) {
        return prices[kit.ordinal()];
    }

    public long[] prices() {
        return prices.clone();
    }

    public void setPrice(TuningKit kit, long cents) {
        prices[kit.ordinal()] = Math.max(0, cents);
        setChanged();
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable Player player) {
        setOwner(player == null ? null : player.getUUID(), player == null ? "" : player.getGameProfile().getName());
    }

    public void setOwner(@Nullable UUID id, String name) {
        owner = id;
        ownerName = id == null ? "" : name;
        setChanged();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** The owner or an admin may stock the bench, set its prices and break it. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    public long earned() {
        return earned;
    }

    public int jobs() {
        return jobs;
    }

    void recordJob(long total) {
        earned += total;
        jobs++;
        setChanged();
    }

    /** Drops everything in stock where the bench stands. */
    void dropStock() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < stock.getSlots(); slot++) {
            ItemStack stack = stock.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                stock.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Stock", stock.serializeNBT(registries));
        tag.putLongArray("Prices", prices);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
        tag.putLong("Earned", earned);
        tag.putInt("Jobs", jobs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Stock")) {
            stock.deserializeNBT(registries, tag.getCompound("Stock"));
        }
        long[] saved = tag.getLongArray("Prices");
        for (int i = 0; i < Math.min(saved.length, prices.length); i++) {
            prices[i] = Math.max(0, saved[i]);
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        earned = tag.getLong("Earned");
        jobs = tag.getInt("Jobs");
    }

    /** Lets kits in but never out. */
    private record InsertOnly(IItemHandler stock) implements IItemHandler {
        @Override
        public int getSlots() {
            return stock.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return stock.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stock.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return stock.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stock.isItemValid(slot, stack);
        }
    }
}
