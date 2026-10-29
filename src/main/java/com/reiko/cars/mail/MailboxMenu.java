package com.reiko.cars.mail;

import com.reiko.cars.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The mailbox screen's server side. The Send tab has nine slots to pack a parcel in (given back if the screen closes
 * before it's sent); sending goes through {@code MailActionPayload}. The owner's Inbox tab has the mail that's come for
 * them. The player's inventory is below either.
 */
public final class MailboxMenu extends AbstractContainerMenu {
    public static final int TAB_SEND = 0;
    public static final int TAB_INBOX = 1;
    public static final int WIDTH = 194;
    public static final int SLOTS_X = 16;

    // Data slots: how many stacks are in the inbox, and parcels in the outbox.
    private static final int INBOX = 0;
    private static final int OUTBOX = 1;
    private static final int DATA = 2;

    @Nullable
    private final MailboxBlockEntity box; // null on the client
    private final SimpleContainer packing = new SimpleContainer(MailboxBlockEntity.SLOTS);
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final boolean owner;
    private final long postage;
    private final ContainerData data;
    /** Where the slots sit: lower for the owner, who has tabs. */
    private final int top;
    /** The tab the screen shows, which decides which slots do (the screen tells the server with a menu button). */
    private int tab = TAB_SEND;

    public MailboxMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, new ItemStackHandler(MailboxBlockEntity.SLOTS), buf.readBlockPos(), buf.readUtf(64),
                buf.readBoolean(), buf.readBoolean(), buf.readVarLong(), new SimpleContainerData(DATA));
    }

    private MailboxMenu(int containerId, Inventory inventory, @Nullable MailboxBlockEntity box, IItemHandler inbox, BlockPos pos,
                        String ownerName, boolean manager, boolean owner, long postage, ContainerData data) {
        super(ModMenus.MAILBOX.get(), containerId);
        this.box = box;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.owner = owner;
        this.postage = postage;
        this.data = data;
        this.top = manager ? 30 : 18;
        int slotsY = slotsY();
        for (int i = 0; i < MailboxBlockEntity.SLOTS; i++) {
            addSlot(new Slot(packing, i, SLOTS_X + i * 18, slotsY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return fitsInParcel(stack);
                }

                @Override
                public boolean isActive() {
                    return tab == TAB_SEND;
                }
            });
        }
        for (int i = 0; i < MailboxBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(inbox, i, SLOTS_X + i * 18, slotsY) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // mail comes in by the mail van, or from the Send tab
                }

                @Override
                public boolean mayPickup(Player player) {
                    return manager;
                }

                @Override
                public boolean isActive() {
                    return manager && tab == TAB_INBOX;
                }
            });
        }
        int inventoryY = inventoryY();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row == 3 ? col : col + (row + 1) * 9;
                addSlot(new Slot(inventory, index, SLOTS_X + col * 18, row == 3 ? inventoryY + 58 : inventoryY + row * 18));
            }
        }
        addDataSlots(data);
    }

    /** What can go in a parcel: anything that could go in a shulker box, and no parcels (no parcels in parcels). */
    public static boolean fitsInParcel(ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems() && !(stack.getItem() instanceof ParcelItem);
    }

    public static void open(ServerPlayer player, MailboxBlockEntity box) {
        Component title = box.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.mailbox")
                : Component.translatable("container.reikocars.mailbox_of", box.ownerName());
        boolean manager = box.canManage(player) && box.owner() != null;
        boolean owner = box.isOwner(player);
        long postage = MailService.postage();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, box), title), buf -> {
            buf.writeBlockPos(box.getBlockPos());
            buf.writeUtf(box.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeVarLong(postage);
        });
    }

    /** The server side of the screen for {@code player} at {@code box} (opened with {@link #open}). */
    public static MailboxMenu create(int containerId, ServerPlayer player, MailboxBlockEntity box) {
        MailboxMenu menu = new MailboxMenu(containerId, player.getInventory(), box, box.inbox(), box.getBlockPos(), box.ownerName(),
                box.canManage(player) && box.owner() != null, box.isOwner(player), MailService.postage(), new SimpleContainerData(DATA));
        menu.refresh();
        return menu;
    }

    private void refresh() {
        if (box != null) {
            data.set(INBOX, box.count(box.inbox()));
            data.set(OUTBOX, box.count(box.outbox()));
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public MailboxBlockEntity box() {
        return box;
    }

    /** The items packed to send. */
    public SimpleContainer packing() {
        return packing;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** Whether the viewer owns the mailbox (or is an admin): they can take out the mail. */
    public boolean isManager() {
        return manager;
    }

    /** Whether the viewer owns the mailbox. */
    public boolean isOwner() {
        return owner;
    }

    /** The postage for a parcel sent by mail van, as it was when the screen opened. */
    public long postage() {
        return postage;
    }

    public int inboxCount() {
        return data.get(INBOX);
    }

    public int outboxCount() {
        return data.get(OUTBOX);
    }

    public int top() {
        return top;
    }

    public int slotsY() {
        return top + 20;
    }

    public int inventoryY() {
        return top + 126;
    }

    /** Client: the tab showing on the screen. */
    public void setTab(int tab) {
        this.tab = tab;
    }

    /** The screen's tabs, so shift-click on the server knows where things go. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != TAB_SEND && id != TAB_INBOX) {
            return false;
        }
        tab = id;
        return true;
    }

    /** Shift-click: from the inventory into the parcel being packed (on the Send tab); from the parcel or inbox back. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int packEnd = MailboxBlockEntity.SLOTS;
        int inboxEnd = packEnd + MailboxBlockEntity.SLOTS;
        if (index < inboxEnd) {
            if (!moveItemStackTo(stack, inboxEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (tab != TAB_SEND || !fitsInParcel(stack) || !moveItemStackTo(stack, 0, packEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return box == null || (!box.isRemoved() && Container.stillValidBlockEntity(box, player, 8.0F));
    }

    /** Whatever was packed and not sent goes back to the player. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (box != null) {
            clearContainer(player, packing);
        }
    }
}
