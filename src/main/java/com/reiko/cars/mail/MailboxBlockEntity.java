package com.reiko.cars.mail;

import com.reiko.cars.registry.ModBlockEntities;
import com.reiko.cars.registry.ModItems;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * A mailbox: its owner, the mail that's come for them (the inbox, which only they can empty) and the parcels waiting
 * for the mail van (the outbox). The flag goes up while there's mail waiting to go out, as a real one's does, so the
 * carrier knows to stop.
 */
public final class MailboxBlockEntity extends BlockEntity {
    public static final int SLOTS = 9;

    private final ItemStackHandler inbox = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final ItemStackHandler outbox = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.PARCEL.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            updateFlag();
        }
    };
    @Nullable
    private UUID owner;
    private String ownerName = "";

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAILBOX.get(), pos, state);
    }

    public ItemStackHandler inbox() {
        return inbox;
    }

    public ItemStackHandler outbox() {
        return outbox;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(@Nullable UUID id, String name) {
        if (level != null && !level.isClientSide && owner != null) {
            MailRegistry.get(level.getServer()).remove(owner, GlobalPos.of(level.dimension(), worldPosition));
        }
        owner = id;
        ownerName = id == null ? "" : name;
        if (level != null && !level.isClientSide && owner != null) {
            MailRegistry.get(level.getServer()).add(owner, GlobalPos.of(level.dimension(), worldPosition));
        }
        setChanged();
    }

    public void setOwner(Player player) {
        setOwner(player.getUUID(), player.getGameProfile().getName());
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** The owner or an admin may empty the inbox and break the mailbox. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** Puts {@code stack} in the inbox; false (and nothing put in) if there's no room. */
    public boolean deliver(ItemStack stack) {
        return insertAll(inbox, stack);
    }

    /** Puts a parcel in the outbox for the mail van; false if there's no room. */
    public boolean post(ItemStack parcel) {
        return insertAll(outbox, parcel);
    }

    /** Whether there's room in the outbox for {@code parcel}. */
    public boolean canPost(ItemStack parcel) {
        return ItemHandlerHelper.insertItem(outbox, parcel.copy(), true).isEmpty();
    }

    private static boolean insertAll(ItemStackHandler handler, ItemStack stack) {
        if (!ItemHandlerHelper.insertItem(handler, stack.copy(), true).isEmpty()) {
            return false;
        }
        ItemHandlerHelper.insertItem(handler, stack.copy(), false);
        return true;
    }

    public int count(ItemStackHandler handler) {
        int count = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /** The flag is up while there's mail waiting to go out. */
    private void updateFlag() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean waiting = count(outbox) > 0;
        if (state.hasProperty(MailboxBlock.FLAG) && state.getValue(MailboxBlock.FLAG) != waiting) {
            level.setBlock(worldPosition, state.setValue(MailboxBlock.FLAG, waiting), Block.UPDATE_ALL);
        }
    }

    /** Drops everything inside where the mailbox stood. */
    void dropContents() {
        if (level == null) {
            return;
        }
        for (ItemStackHandler handler : new ItemStackHandler[] {inbox, outbox}) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack.copy());
                    handler.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inbox", inbox.serializeNBT(registries));
        tag.put("Outbox", outbox.serializeNBT(registries));
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inbox")) {
            inbox.deserializeNBT(registries, tag.getCompound("Inbox"));
        }
        if (tag.contains("Outbox")) {
            outbox.deserializeNBT(registries, tag.getCompound("Outbox"));
        }
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
    }
}
