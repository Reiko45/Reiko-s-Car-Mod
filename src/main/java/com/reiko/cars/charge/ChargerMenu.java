package com.reiko.cars.charge;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
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
 * The charging screen's server side. It has no slots: charging and price changes go through
 * {@code ChargerActionPayload}. The station's store and the car at the cable are kept live through data slots, so the
 * screen follows along as power comes in or a car pulls up.
 */
public final class ChargerMenu extends AbstractContainerMenu {
    // Data slots travel as shorts, so power is sent in hundreds of MJ (up to 3.2 million MJ).
    private static final int UNIT = 100;
    private static final int STOCK = 0;
    private static final int CAR_TYPE = 1;
    private static final int CAR_CHARGE_PERMILLE = 2;
    private static final int CAR_ROOM = 3;
    /** 1 if the car at the cable is electric. */
    private static final int CAR_ELECTRIC = 4;
    private static final int SLOTS = 5;

    @Nullable
    private final ChargingStationBlockEntity station; // null on the client
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    /** The viewer owns the station, so charges for free. */
    private final boolean owner;
    private final long price;
    private final long capacity;
    private final long mjSold;
    private final long earned;
    private final ContainerData data;
    @Nullable
    private ServerPlayer viewer;
    @Nullable
    private CarEntity car;

    public ChargerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarLong(), new SimpleContainerData(SLOTS));
    }

    private ChargerMenu(int containerId, @Nullable ChargingStationBlockEntity station, BlockPos pos, String ownerName, boolean manager,
                        boolean owner, long price, long capacity, long mjSold, long earned, ContainerData data) {
        super(ModMenus.CHARGER.get(), containerId);
        this.station = station;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.owner = owner;
        this.price = price;
        this.capacity = capacity;
        this.mjSold = mjSold;
        this.earned = earned;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, ChargingStationBlockEntity station) {
        Component title = station.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.charging_station")
                : Component.translatable("container.reikocars.charging_station_of", station.ownerName());
        boolean manager = station.canManage(player);
        boolean owner = station.isOwner(player);
        long price = station.price();
        long capacity = station.capacityMj();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
            ChargerMenu menu = new ChargerMenu(id, station, station.getBlockPos(), station.ownerName(), manager, owner, price, capacity,
                    station.mjSold(), station.earned(), new SimpleContainerData(SLOTS));
            menu.viewer = player;
            menu.refresh();
            return menu;
        }, title), buf -> {
            buf.writeBlockPos(station.getBlockPos());
            buf.writeUtf(station.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeVarLong(price);
            buf.writeVarLong(capacity);
            buf.writeVarLong(station.mjSold());
            buf.writeVarLong(station.earned());
        });
    }

    /** Finds the car at the cable again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (station == null || viewer == null) {
            return;
        }
        if (car == null || !ChargerService.inReach(station, car) || !car.canBeUsedBy(viewer)
                || (viewer.getVehicle() instanceof CarEntity riding && riding != car)) {
            car = ChargerService.findCar(station, viewer);
        }
        data.set(STOCK, units(station.storedMj()));
        data.set(CAR_TYPE, car == null ? -1 : car.carType().ordinal());
        data.set(CAR_CHARGE_PERMILLE, car == null ? 0 : Math.round(car.fuelFraction() * 1000));
        data.set(CAR_ROOM, car == null ? 0 : units(ChargerService.room(car)));
        data.set(CAR_ELECTRIC, car != null && car.carType().isElectric() ? 1 : 0);
    }

    private static int units(long mj) {
        return (int) Math.max(0, Math.min(Short.MAX_VALUE, mj / UNIT));
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public ChargingStationBlockEntity station() {
        return station;
    }

    /** The car the cable reaches, server side. */
    @Nullable
    public CarEntity car() {
        return car;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** Whether the viewer owns the station (or is an admin): they can set the price. */
    public boolean isManager() {
        return manager;
    }

    /** Whether the viewer owns the station, and so charges for free. */
    public boolean isOwner() {
        return owner;
    }

    /** Cents per 1,000 MJ, as it was when the screen opened. Sent back with each charge so a price change is caught. */
    public long price() {
        return price;
    }

    /** MJ the station holds when full. */
    public long capacity() {
        return capacity;
    }

    public long mjSold() {
        return mjSold;
    }

    public long earned() {
        return earned;
    }

    /** MJ stored, to the nearest hundred below. */
    public long stock() {
        return (long) data.get(STOCK) * UNIT;
    }

    @Nullable
    public CarType carType() {
        int ordinal = data.get(CAR_TYPE);
        return ordinal >= 0 && ordinal < CarType.values().length ? CarType.values()[ordinal] : null;
    }

    /** 0 to 1. */
    public float carCharge() {
        return data.get(CAR_CHARGE_PERMILLE) / 1000.0F;
    }

    /** Whether the car at the cable is electric. */
    public boolean carElectric() {
        return data.get(CAR_ELECTRIC) == 1;
    }

    /** MJ the car's battery can still take, to the nearest hundred below. */
    public long carRoom() {
        return (long) data.get(CAR_ROOM) * UNIT;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return station == null || (!station.isRemoved() && Container.stillValidBlockEntity(station, player, 6.0F));
    }
}
