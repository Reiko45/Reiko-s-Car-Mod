package com.reiko.cars.fare;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The pay screen a bus passenger gets when they sit down: the fare, paid by card or cash (through
 * {@code FareActionPayload}), or they get off. Closing it without paying puts them off the bus.
 */
public final class FareMenu extends AbstractContainerMenu {
    @Nullable
    private final CarEntity bus; // null on the client
    @Nullable
    private final ServerPlayer rider;
    private final Component vehicle;
    private final String driverName;
    private final long fare;

    public FareMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, null, Component.translatable(buf.readUtf(128)), buf.readUtf(64), buf.readVarLong());
    }

    private FareMenu(int containerId, @Nullable CarEntity bus, @Nullable ServerPlayer rider, Component vehicle, String driverName, long fare) {
        super(ModMenus.FARE.get(), containerId);
        this.bus = bus;
        this.rider = rider;
        this.vehicle = vehicle;
        this.driverName = driverName;
        this.fare = fare;
    }

    public static void open(ServerPlayer rider, CarEntity bus, Player driver) {
        String driverName = driver.getGameProfile().getName();
        long fare = bus.fares().start();
        rider.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, rider, bus, driverName),
                Component.translatable("container.reikocars.fare", bus.kindName())), buf -> {
                    buf.writeUtf(bus.nameKey(), 128);
                    buf.writeUtf(driverName, 64);
                    buf.writeVarLong(fare);
                });
    }

    /** The server side of the pay screen (opened with {@link #open}). */
    public static FareMenu create(int containerId, ServerPlayer rider, CarEntity bus, String driverName) {
        return new FareMenu(containerId, bus, rider, bus.kindName(), driverName, bus.fares().start());
    }

    @Nullable
    public CarEntity bus() {
        return bus;
    }

    public Component vehicle() {
        return vehicle;
    }

    public String driverName() {
        return driverName;
    }

    /** The fare, before tax, as it was when the screen opened. */
    public long fare() {
        return fare;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return bus == null || (bus.isAlive() && bus.hasPassenger(player));
    }

    /** Closed: anyone who didn't pay gets off. */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (bus != null && player instanceof ServerPlayer serverPlayer) {
            bus.fares().screenClosed(serverPlayer);
        }
    }
}
