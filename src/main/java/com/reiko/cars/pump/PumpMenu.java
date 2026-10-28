package com.reiko.cars.pump;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * The pump screen's server side. It has no slots: fill-ups and price changes go through {@code PumpActionPayload}.
 * The fuel in the pump and the car at the hose are kept live through data slots, so the screen follows along as
 * fuel arrives by pipe or a car pulls up.
 */
public final class PumpMenu extends AbstractContainerMenu {
    // Data slots. They travel as shorts, so amounts are capped at Short.MAX_VALUE millibuckets (32 buckets).
    private static final int STOCK = 0;
    private static final int FLUID = 1;
    private static final int CAR_TYPE = 2;
    private static final int CAR_FUEL_PERMILLE = 3;
    private static final int CAR_ROOM = 4;
    private static final int SLOTS = 5;

    @Nullable
    private final GasPumpBlockEntity pump; // null on the client
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    /** The viewer owns the pump, so fills up for free. */
    private final boolean owner;
    private final long price;
    private final int capacity;
    private final long millibucketsSold;
    private final long earned;
    private final ContainerData data;
    @Nullable
    private ServerPlayer viewer;
    @Nullable
    private CarEntity car;

    public PumpMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readBoolean(), buf.readVarLong(),
                buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), new SimpleContainerData(SLOTS));
    }

    private PumpMenu(int containerId, @Nullable GasPumpBlockEntity pump, BlockPos pos, String ownerName, boolean manager,
                     boolean owner, long price, int capacity, long millibucketsSold, long earned, ContainerData data) {
        super(ModMenus.PUMP.get(), containerId);
        this.pump = pump;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.owner = owner;
        this.price = price;
        this.capacity = capacity;
        this.millibucketsSold = millibucketsSold;
        this.earned = earned;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, GasPumpBlockEntity pump) {
        Component title = pump.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.gas_pump")
                : Component.translatable("container.reikocars.gas_pump_of", pump.ownerName());
        boolean manager = pump.canManage(player);
        boolean owner = pump.isOwner(player);
        long price = pump.price();
        int capacity = pump.tank().getCapacity();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
            PumpMenu menu = new PumpMenu(id, pump, pump.getBlockPos(), pump.ownerName(), manager, owner, price, capacity,
                    pump.millibucketsSold(), pump.earned(), new SimpleContainerData(SLOTS));
            menu.viewer = player;
            menu.refresh();
            return menu;
        }, title), buf -> {
            buf.writeBlockPos(pump.getBlockPos());
            buf.writeUtf(pump.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeVarLong(price);
            buf.writeVarInt(capacity);
            buf.writeVarLong(pump.millibucketsSold());
            buf.writeVarLong(pump.earned());
        });
    }

    /** Finds the car at the hose again and updates the data slots. Server side, every tick. */
    private void refresh() {
        if (pump == null || viewer == null) {
            return;
        }
        if (car == null || !PumpService.inReach(pump, car) || !car.canBeUsedBy(viewer)
                || (viewer.getVehicle() instanceof CarEntity riding && riding != car)) {
            car = PumpService.findCar(pump, viewer);
        }
        data.set(STOCK, clamp(pump.fuel().getAmount()));
        data.set(FLUID, pump.fuel().isEmpty() ? 0 : BuiltInRegistries.FLUID.getId(pump.fuel().getFluid()));
        data.set(CAR_TYPE, car == null ? -1 : car.carType().ordinal());
        data.set(CAR_FUEL_PERMILLE, car == null ? 0 : Math.round(car.fuelFraction() * 1000));
        data.set(CAR_ROOM, car == null ? 0 : clamp(PumpService.room(pump, car)));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(Short.MAX_VALUE, value));
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public GasPumpBlockEntity pump() {
        return pump;
    }

    /** The car the hose reaches, server side. */
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

    /** Whether the viewer owns the pump (or is an admin): they fill up free and can set the price. */
    public boolean isManager() {
        return manager;
    }

    /** Whether the viewer owns the pump, and so pumps for free. */
    public boolean isOwner() {
        return owner;
    }

    /** Cents per bucket, as it was when the screen opened. Sent back with each fill-up so a price change is caught. */
    public long price() {
        return price;
    }

    public int capacity() {
        return capacity;
    }

    public long millibucketsSold() {
        return millibucketsSold;
    }

    public long earned() {
        return earned;
    }

    public int stock() {
        return data.get(STOCK);
    }

    public Fluid fluid() {
        return stock() > 0 ? BuiltInRegistries.FLUID.byId(data.get(FLUID)) : Fluids.EMPTY;
    }

    @Nullable
    public CarType carType() {
        int ordinal = data.get(CAR_TYPE);
        return ordinal >= 0 && ordinal < CarType.values().length ? CarType.values()[ordinal] : null;
    }

    /** 0 to 1. */
    public float carFuel() {
        return data.get(CAR_FUEL_PERMILLE) / 1000.0F;
    }

    /** Millibuckets of the pump's fuel the car can still take. */
    public int carRoom() {
        return data.get(CAR_ROOM);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return pump == null || (!pump.isRemoved() && Container.stillValidBlockEntity(pump, player, 6.0F));
    }
}
