package com.reiko.cars.garage;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * A driver's pay screen at a parking garage's door: to park, some hours or days; or, leaving after their time's up, the
 * hours they've overstayed. By card or cash.
 */
public final class ParkingMenu extends AbstractContainerMenu {
    private final BlockPos anchor;
    private final boolean leaving;
    private final long hourPrice;
    private final long dayPrice;
    private final long fee;
    private final int overHours;
    private final String name;

    public ParkingMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), buf.readUtf(64));
    }

    private ParkingMenu(int containerId, BlockPos anchor, boolean leaving, long hourPrice, long dayPrice, long fee, int overHours, String name) {
        super(ModMenus.PARKING.get(), containerId);
        this.anchor = anchor;
        this.leaving = leaving;
        this.hourPrice = hourPrice;
        this.dayPrice = dayPrice;
        this.fee = fee;
        this.overHours = overHours;
        this.name = name;
    }

    public static void open(ServerPlayer driver, GarageDoorBlockEntity door, CarEntity car, boolean leaving) {
        BlockPos anchor = door.getBlockPos();
        long hourPrice = door.settings().hourPrice();
        long dayPrice = door.settings().dayPrice();
        long fee = leaving ? ParkingService.overstay(car, door) : 0L;
        int overHours = leaving ? ParkingService.overstayHours(car, door) : 0;
        String name = door.settings().name().isEmpty() ? door.ownerName() : door.settings().name();
        driver.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ParkingMenu(id, anchor, leaving, hourPrice, dayPrice, fee, overHours, name),
                Component.translatable(leaving ? "container.reikocars.parking_leave" : "container.reikocars.parking", name)), buf -> {
                    buf.writeBlockPos(anchor);
                    buf.writeBoolean(leaving);
                    buf.writeVarLong(hourPrice);
                    buf.writeVarLong(dayPrice);
                    buf.writeVarLong(fee);
                    buf.writeVarInt(overHours);
                    buf.writeUtf(name, 64);
                });
    }

    public BlockPos anchor() {
        return anchor;
    }

    /** Paying an overstay to get out, rather than to park. */
    public boolean leaving() {
        return leaving;
    }

    /** What option {@code option} costs before tax, as the prices were when the screen opened. */
    public long price(int option) {
        return ParkingService.price(new GarageSettings("", -1, false, false, false, 0L, true, hourPrice, dayPrice), option);
    }

    /** The overstay, before tax. */
    public long fee() {
        return fee;
    }

    public int overHours() {
        return overHours;
    }

    public String name() {
        return name;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(anchor.getCenter()) < 24.0 * 24.0;
    }
}
