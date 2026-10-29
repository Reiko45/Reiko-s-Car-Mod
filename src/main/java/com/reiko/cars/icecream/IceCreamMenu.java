package com.reiko.cars.icecream;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * The ice cream truck's shop screen, server side: the menu (what's priced and in stock, picked with the arrows) for
 * customers, every kind in the freezer for its owner, who prices them. The picked item is kept live in data slots.
 */
public final class IceCreamMenu extends AbstractContainerMenu {
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;

    // Data slots (16 bits each): how many kinds, which one's picked, its item id (two slots), how many are left, and
    // its price (four slots; -1 when it isn't priced).
    private static final int COUNT = 0;
    private static final int INDEX = 1;
    private static final int ITEM = 2;
    private static final int STOCK = 4;
    private static final int PRICE = 5;
    private static final int SLOTS = 9;

    @Nullable
    private final CarEntity truck; // null on the client
    private final int truckId;
    private final String ownerName;
    private final boolean owner;
    private final ContainerData data;
    @Nullable
    private Item selected;

    public IceCreamMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readVarInt(), buf.readUtf(64), buf.readBoolean(), new SimpleContainerData(SLOTS));
    }

    private IceCreamMenu(int containerId, @Nullable CarEntity truck, int truckId, String ownerName, boolean owner, ContainerData data) {
        super(ModMenus.ICE_CREAM.get(), containerId);
        this.truck = truck;
        this.truckId = truckId;
        this.ownerName = ownerName;
        this.owner = owner;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, CarEntity truck) {
        Component title = Component.translatable("container.reikocars.ice_cream_of", truck.ownerName());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, truck), title), buf -> {
            buf.writeVarInt(truck.getId());
            buf.writeUtf(truck.ownerName(), 64);
            buf.writeBoolean(truck.isOwner(player));
        });
    }

    public static IceCreamMenu create(int containerId, ServerPlayer player, CarEntity truck) {
        IceCreamMenu menu = new IceCreamMenu(containerId, truck, truck.getId(), truck.ownerName(), truck.isOwner(player), new SimpleContainerData(SLOTS));
        menu.refresh();
        return menu;
    }

    private List<Item> items() {
        return truck == null ? List.of() : IceCreamService.menu(truck, owner);
    }

    /** Takes stock again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (truck == null) {
            return;
        }
        List<Item> items = items();
        if (selected == null || !items.contains(selected)) {
            selected = items.isEmpty() ? null : items.get(0);
        }
        data.set(COUNT, items.size());
        data.set(INDEX, selected == null ? 0 : items.indexOf(selected));
        int id = selected == null ? -1 : BuiltInRegistries.ITEM.getId(selected);
        data.set(ITEM, (id >> 16) & 0xFFFF);
        data.set(ITEM + 1, id & 0xFFFF);
        data.set(STOCK, selected == null ? 0 : Math.min(Short.MAX_VALUE, IceCreamService.stock(truck).getOrDefault(selected, 0)));
        long price = selected == null ? -1 : IceCreamService.price(truck, selected);
        for (int i = 0; i < 4; i++) {
            data.set(PRICE + i, (int) ((price >> (16 * i)) & 0xFFFF));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PREVIOUS && id != NEXT) {
            return false;
        }
        List<Item> items = items();
        if (items.isEmpty()) {
            return false;
        }
        int index = selected == null ? 0 : items.indexOf(selected);
        selected = items.get(Math.floorMod(index + (id == NEXT ? 1 : -1), items.size()));
        refresh();
        return true;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public CarEntity truck() {
        return truck;
    }

    /** The item picked, server side. */
    @Nullable
    public Item selected() {
        return selected;
    }

    public int truckId() {
        return truckId;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean isOwner() {
        return owner;
    }

    public int count() {
        return data.get(COUNT);
    }

    public int index() {
        return data.get(INDEX);
    }

    /** The item picked, as the client sees it. */
    public ItemStack item() {
        int id = ((data.get(ITEM) & 0xFFFF) << 16) | (data.get(ITEM + 1) & 0xFFFF);
        if (count() == 0 || id < 0) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.byId(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public int stock() {
        return data.get(STOCK);
    }

    /** Its price in cents, or -1 if it isn't priced. */
    public long price() {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (data.get(PRICE + i) & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return truck == null || (truck.isAlive() && player.distanceToSqr(truck) < 8.0 * 8.0);
    }
}
