package com.reiko.cars.rental;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
import java.util.ArrayList;
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
 * A rental desk's screen, server side. Customers browse the fleet, picked with the arrows, and rent a car for some
 * days; the owner sees the fleet and the cars out on rent (Fleet) and sets a day's rent for each kind of car (Prices).
 * The car picked is kept live through data slots; actions go through {@code LotActionPayload}.
 */
public final class RentalMenu extends AbstractContainerMenu {
    public static final int TAB_FLEET = 0;
    public static final int TAB_PRICES = 1;
    public static final int PREVIOUS = 10;
    public static final int NEXT = 11;

    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int COLOR = 4;
    private static final int CONDITION = 6;
    private static final int FUEL = 7;
    private static final int RENTED = 8;
    private static final int MINUTES_LEFT = 9;
    private static final int SLOTS = 10;

    @Nullable
    private final RentalDeskBlockEntity desk; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final long[] prices;
    private final long earned;
    private final int rentals;
    private final ContainerData data;
    @Nullable
    private CarEntity selected;

    public RentalMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readLongArray(), buf.readVarLong(), buf.readVarInt(),
                new SimpleContainerData(SLOTS));
    }

    private RentalMenu(int containerId, @Nullable RentalDeskBlockEntity desk, BlockPos pos, String ownerName, boolean manager, long[] prices,
                       long earned, int rentals, ContainerData data) {
        super(ModMenus.RENTAL.get(), containerId);
        this.desk = desk;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.prices = prices.length == CarType.values().length ? prices : new long[CarType.values().length];
        this.earned = earned;
        this.rentals = rentals;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, RentalDeskBlockEntity desk) {
        Component title = desk.ownerName().isEmpty() ? Component.translatable("container.reikocars.rental")
                : Component.translatable("container.reikocars.rental_of", desk.ownerName());
        boolean manager = desk.isOwner(player);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, desk), title), buf -> {
            buf.writeBlockPos(desk.getBlockPos());
            buf.writeUtf(desk.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeLongArray(desk.prices());
            buf.writeVarLong(desk.earned());
            buf.writeVarInt(desk.rentals());
        });
    }

    public static RentalMenu create(int containerId, ServerPlayer player, RentalDeskBlockEntity desk) {
        RentalMenu menu = new RentalMenu(containerId, desk, desk.getBlockPos(), desk.ownerName(), desk.isOwner(player), desk.prices(),
                desk.earned(), desk.rentals(), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    /** The cars to pick from: the fleet, and for the owner the cars out on rent too. */
    private List<CarEntity> cars() {
        if (desk == null || viewer == null) {
            return List.of();
        }
        if (!manager) {
            return RentalService.fleet(desk);
        }
        List<CarEntity> cars = new ArrayList<>(RentalService.fleet(desk));
        cars.addAll(RentalService.rentedOut(desk));
        return cars;
    }

    /** Finds the cars again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (desk == null || viewer == null) {
            return;
        }
        List<CarEntity> cars = cars();
        if (selected == null || !cars.contains(selected)) {
            selected = cars.isEmpty() ? null : cars.get(0);
        }
        Rental rental = selected == null ? null : selected.rental();
        data.set(KIND, selected == null ? 0 : 1);
        data.set(TYPE, selected == null ? 0 : selected.carType().ordinal());
        data.set(INDEX, selected == null ? 0 : cars.indexOf(selected));
        data.set(COUNT, cars.size());
        int color = selected == null ? 0 : selected.color();
        data.set(COLOR, (color >> 16) & 0xFFFF);
        data.set(COLOR + 1, color & 0xFFFF);
        data.set(CONDITION, selected == null ? 0 : Math.round(selected.condition().overall(selected::hasPart) * 1000));
        data.set(FUEL, selected == null ? 0 : Math.round(selected.fuelFraction() * 1000));
        data.set(RENTED, rental != null ? 1 : 0);
        long left = rental == null ? 0 : rental.until() - desk.getLevel().getGameTime();
        data.set(MINUTES_LEFT, (int) Math.max(0, Math.min(30000, (left + 1_199) / 1_200)));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
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
    public RentalDeskBlockEntity desk() {
        return desk;
    }

    @Nullable
    public CarEntity selected() {
        return selected;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** The desk's owner: they get the Fleet and Prices tabs. */
    public boolean isManager() {
        return manager;
    }

    /** A day's rent for {@code type} when the screen opened, in cents. */
    public long price(CarType type) {
        return prices[type.ordinal()];
    }

    public long earned() {
        return earned;
    }

    public int rentals() {
        return rentals;
    }

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

    public int carCondition() {
        return data.get(CONDITION);
    }

    public int carFuel() {
        return data.get(FUEL);
    }

    /** Whether the picked car is out on rent (the owner's view). */
    public boolean carRented() {
        return data.get(RENTED) != 0;
    }

    /** Minutes left on its rental. */
    public int minutesLeft() {
        return data.get(MINUTES_LEFT);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return desk == null || (!desk.isRemoved() && Container.stillValidBlockEntity(desk, player, 8.0F));
    }
}
