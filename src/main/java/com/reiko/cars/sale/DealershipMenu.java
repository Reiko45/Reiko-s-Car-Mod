package com.reiko.cars.sale;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A dealership's screen, server side. Customers get Buy (the cars for sale on the lot) and Trade in (their own cars
 * on the lot, and what the dealer will pay); the owner gets Lot (their cars there, to price) and Trade-ins (what they'll
 * pay for each kind of car). The car picked on the first three, picked with the screen's arrows, is kept live through
 * data slots; actions go through {@code SaleActionPayload}.
 */
public final class DealershipMenu extends AbstractContainerMenu {
    public static final int TAB_BUY = 0;
    public static final int TAB_TRADE = 1;
    public static final int TAB_LOT = 2;
    public static final int TAB_OFFERS = 3;
    /** Menu buttons: tabs 0 to 3, and these to pick the previous or next car. */
    public static final int PREVIOUS = 10;
    public static final int NEXT = 11;

    // Data slots (16 bits each): the tab, the car picked (whether there is one, its type), which of how many it is,
    // its colour (two slots), condition and fuel in thousandths, its price and the trade-in offer (four slots each).
    private static final int TAB = 0;
    private static final int KIND = 1;
    private static final int TYPE = 2;
    private static final int INDEX = 3;
    private static final int COUNT = 4;
    private static final int COLOR = 5;
    private static final int CONDITION = 7;
    private static final int FUEL = 8;
    private static final int PRICE = 9;
    private static final int OFFER = 13;
    /** What's owed on the picked car's loan (Trade in tab), or -1 if it has none. */
    private static final int OWED = 17;
    /** How dirty the picked car is, in percent. */
    private static final int DIRT = 21;
    private static final int SLOTS = 22;

    @Nullable
    private final DealershipBlockEntity lot; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final long[] offers;
    private final long earned;
    private final int sold;
    private final long spent;
    private final int bought;
    private final ContainerData data;
    private int tab;
    @Nullable
    private CarEntity selected;

    public DealershipMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readLongArray(), buf.readVarLong(),
                buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), new SimpleContainerData(SLOTS));
    }

    private DealershipMenu(int containerId, @Nullable DealershipBlockEntity lot, BlockPos pos, String ownerName, boolean manager,
                           long[] offers, long earned, int sold, long spent, int bought, ContainerData data) {
        super(ModMenus.DEALERSHIP.get(), containerId);
        this.lot = lot;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.offers = offers.length == CarType.values().length ? offers : new long[CarType.values().length];
        this.earned = earned;
        this.sold = sold;
        this.spent = spent;
        this.bought = bought;
        this.data = data;
        this.tab = manager ? TAB_LOT : TAB_BUY;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, DealershipBlockEntity lot) {
        Component title = lot.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.dealership")
                : Component.translatable("container.reikocars.dealership_of", lot.ownerName());
        boolean manager = lot.isOwner(player); // an admin who doesn't own it shops like anyone else
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, lot), title), buf -> {
            buf.writeBlockPos(lot.getBlockPos());
            buf.writeUtf(lot.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeLongArray(lot.offers());
            buf.writeVarLong(lot.earned());
            buf.writeVarInt(lot.sold());
            buf.writeVarLong(lot.spent());
            buf.writeVarInt(lot.bought());
        });
    }

    public static DealershipMenu create(int containerId, ServerPlayer player, DealershipBlockEntity lot) {
        DealershipMenu menu = new DealershipMenu(containerId, lot, lot.getBlockPos(), lot.ownerName(), lot.isOwner(player),
                lot.offers(), lot.earned(), lot.sold(), lot.spent(), lot.bought(), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    /** The cars on the current tab, server side. */
    private List<CarEntity> cars() {
        if (lot == null || viewer == null) {
            return List.of();
        }
        return switch (tab) {
            case TAB_BUY -> SaleService.forSale(lot);
            case TAB_TRADE -> SaleService.tradeable(lot, viewer);
            case TAB_LOT -> SaleService.stock(lot);
            default -> List.of();
        };
    }

    /** Finds the cars again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (lot == null || viewer == null) {
            return;
        }
        List<CarEntity> cars = cars();
        if (selected == null || !cars.contains(selected)) {
            selected = cars.isEmpty() ? null : cars.get(0);
        }
        data.set(TAB, tab);
        data.set(KIND, selected == null ? 0 : 1);
        data.set(TYPE, selected == null ? 0 : selected.carType().ordinal());
        data.set(INDEX, selected == null ? 0 : cars.indexOf(selected));
        data.set(COUNT, cars.size());
        int color = selected == null ? 0 : selected.color();
        data.set(COLOR, (color >> 16) & 0xFFFF);
        data.set(COLOR + 1, color & 0xFFFF);
        data.set(CONDITION, selected == null ? 0 : Math.round(selected.condition().overall(selected::hasPart) * 1000));
        data.set(FUEL, selected == null ? 0 : Math.round(selected.fuelFraction() * 1000));
        data.set(DIRT, selected == null ? 0 : Math.round(selected.dirt() * 100));
        putLong(PRICE, selected == null ? -1 : selected.salePrice());
        putLong(OFFER, selected == null || tab != TAB_TRADE ? 0 : SaleService.offerFor(lot, selected));
        putLong(OWED, selected == null || tab != TAB_TRADE ? -1 : com.reiko.cars.finance.LoanService.owed(selected));
    }

    private void putLong(int slot, long value) {
        for (int i = 0; i < 4; i++) {
            data.set(slot + i, (int) ((value >> (16 * i)) & 0xFFFF));
        }
    }

    private long getLong(int slot) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (data.get(slot + i) & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id >= TAB_BUY && id <= TAB_OFFERS) {
            if ((id == TAB_LOT || id == TAB_OFFERS) && !manager) {
                return false;
            }
            tab = id;
            selected = null;
            refresh();
            return true;
        }
        if (id == PREVIOUS || id == NEXT) {
            List<CarEntity> cars = cars();
            if (cars.isEmpty()) {
                return false;
            }
            int index = selected == null ? 0 : cars.indexOf(selected);
            selected = cars.get(Math.floorMod(index + (id == NEXT ? 1 : -1), cars.size()));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public DealershipBlockEntity lot() {
        return lot;
    }

    /** The car picked on the current tab, server side. */
    @Nullable
    public CarEntity selected() {
        return selected;
    }

    public int tab() {
        return tab;
    }

    /** Client: the tab showing (the server's is set with a menu button). */
    public void setTab(int tab) {
        this.tab = tab;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** The owner: they get the Lot and Trade-ins tabs. */
    public boolean isManager() {
        return manager;
    }

    /** What the dealer pays for each kind of car, by ordinal, when the screen opened. */
    public long offer(CarType type) {
        return offers[type.ordinal()];
    }

    public long earned() {
        return earned;
    }

    public int sold() {
        return sold;
    }

    public long spent() {
        return spent;
    }

    public int bought() {
        return bought;
    }

    // ---- the car picked, client side ----

    public boolean hasCar() {
        return data.get(KIND) != 0;
    }

    @Nullable
    public CarType carType() {
        int type = data.get(TYPE);
        return hasCar() && type >= 0 && type < CarType.values().length ? CarType.values()[type] : null;
    }

    public int carIndex() {
        return data.get(INDEX);
    }

    public int carCount() {
        return data.get(COUNT);
    }

    public int carColor() {
        return ((data.get(COLOR) & 0xFFFF) << 16) | (data.get(COLOR + 1) & 0xFFFF);
    }

    /** Its overall condition, in thousandths. */
    public int carCondition() {
        return data.get(CONDITION);
    }

    public int carFuel() {
        return data.get(FUEL);
    }

    /** How dirty it is, in percent. */
    public int carDirt() {
        return data.get(DIRT);
    }

    /** Its price, in cents, or -1 if it isn't for sale. */
    public long carPrice() {
        return getLong(PRICE);
    }

    /** What the dealer will pay for it (Trade in tab). */
    public long carOffer() {
        return getLong(OFFER);
    }

    /** What's owed on its loan (Trade in tab), or -1 if it has none. */
    public long carOwed() {
        return getLong(OWED);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return lot == null || (!lot.isRemoved() && Container.stillValidBlockEntity(lot, player, 8.0F));
    }
}
