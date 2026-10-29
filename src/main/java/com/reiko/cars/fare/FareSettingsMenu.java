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
 * The owner's fare screen for a city bus or taxi (sneak and right-click it with an empty hand): its fares, and what
 * it's earned. Changes go through {@code FareActionPayload}.
 */
public final class FareSettingsMenu extends AbstractContainerMenu {
    @Nullable
    private final CarEntity car; // null on the client
    private final boolean metered;
    private final long start;
    private final long per100;
    private final long earned;
    private final int rides;

    public FareSettingsMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
    }

    private FareSettingsMenu(int containerId, @Nullable CarEntity car, boolean metered, long start, long per100, long earned, int rides) {
        super(ModMenus.FARE_SETTINGS.get(), containerId);
        this.car = car;
        this.metered = metered;
        this.start = start;
        this.per100 = per100;
        this.earned = earned;
        this.rides = rides;
    }

    public static void open(ServerPlayer player, CarEntity car) {
        CarFares fares = car.fares();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, car),
                Component.translatable("container.reikocars.fares_of", car.kindName())), buf -> {
                    buf.writeBoolean(car.carType().isMetered());
                    buf.writeVarLong(fares.start());
                    buf.writeVarLong(fares.per100());
                    buf.writeVarLong(fares.earned());
                    buf.writeVarInt(fares.rides());
                });
    }

    public static FareSettingsMenu create(int containerId, CarEntity car) {
        CarFares fares = car.fares();
        return new FareSettingsMenu(containerId, car, car.carType().isMetered(), fares.start(), fares.per100(), fares.earned(), fares.rides());
    }

    @Nullable
    public CarEntity car() {
        return car;
    }

    /** A taxi (a starting fare and a rate per 100 blocks), rather than a bus (one fare). */
    public boolean isMetered() {
        return metered;
    }

    public long start() {
        return start;
    }

    public long per100() {
        return per100;
    }

    public long earned() {
        return earned;
    }

    public int rides() {
        return rides;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return car == null || (car.isAlive() && player.distanceToSqr(car) < 64.0);
    }
}
