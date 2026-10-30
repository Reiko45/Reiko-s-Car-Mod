package com.reiko.cars.radio;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A car's radio: its buttons, and the disc slot, with the rider's inventory below. */
public final class RadioMenu extends AbstractContainerMenu {
    public static final int DISC_X = 152;
    public static final int DISC_Y = 76;
    public static final int INVENTORY_Y = 106;

    @Nullable
    private final CarEntity car;
    private final int carId;

    public RadioMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readVarInt(), new SimpleContainer(1), null);
    }

    private RadioMenu(int containerId, Inventory inventory, int carId, Container disc, @Nullable CarEntity car) {
        super(ModMenus.RADIO.get(), containerId);
        this.car = car;
        this.carId = carId;
        addSlot(new Slot(disc, 0, DISC_X, DISC_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return CarRadio.playable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
    }

    public static void open(ServerPlayer player, CarEntity car) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, inventory, car),
                Component.translatable("container.reikocars.radio")), buf -> buf.writeVarInt(car.getId()));
    }

    public static RadioMenu create(int containerId, Inventory inventory, CarEntity car) {
        return new RadioMenu(containerId, inventory, car.getId(), car.radioDisc(), car);
    }

    /** The car, on the server. */
    @Nullable
    public CarEntity car() {
        return car;
    }

    public int carId() {
        return carId;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return car != null && CarRadio.press(car, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!CarRadio.playable(stack) || !moveItemStackTo(stack, 0, 1, false)) {
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
        return car == null || (car.isAlive() && player.getVehicle() == car);
    }
}
