package com.reiko.cars.road;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import org.jetbrains.annotations.Nullable;

/**
 * A road paver's screen: its hopper (what it lays: asphalt, paint, slabs, fill, poles and lamps) and its settings,
 * which the screen's buttons step one at a time ({@link #clickMenuButton}: button {@code 2 * setting} steps a setting
 * back, {@code 2 * setting + 1} forward).
 */
public final class PaverMenu extends AbstractContainerMenu {
    public static final int LEFT = 8;
    public static final int TOP = 18;
    public static final int ROWS = 3;
    /** The data slots: the settings, then whether it's paving, then the money spent (in cents, two 15-bit halves). */
    public static final int PAVING = PaverSettings.COUNT;
    public static final int SPENT_LOW = PaverSettings.COUNT + 1;
    public static final int SPENT_HIGH = PaverSettings.COUNT + 2;
    public static final int DATA_COUNT = PaverSettings.COUNT + 3;

    @Nullable
    private final CarEntity paver; // null on the client
    private final Container hopper;
    private final ContainerData data;

    public PaverMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, new SimpleContainer(ROWS * 9), new SimpleContainerData(DATA_COUNT));
    }

    private PaverMenu(int containerId, Inventory inventory, @Nullable CarEntity paver, Container hopper, ContainerData data) {
        super(ModMenus.PAVER.get(), containerId);
        this.paver = paver;
        this.hopper = hopper;
        this.data = data;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(hopper, col + row * 9, LEFT + col * 18, TOP + row * 18));
            }
        }
        int inventoryTop = inventoryTop();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, LEFT + col * 18, inventoryTop + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, LEFT + col * 18, inventoryTop + 58));
        }
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, CarEntity paver) {
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                if (index < PaverSettings.COUNT) {
                    return paver.paverSettings().get(index);
                }
                long spent = paver.paverState().spent();
                return switch (index) {
                    case PAVING -> paver.paving() ? 1 : 0;
                    case SPENT_LOW -> (int) (spent & 0x7FFF);
                    case SPENT_HIGH -> (int) Math.min(0x7FFF, spent >> 15);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PaverMenu(id, inventory, paver, paver.bed(), data), paver.getDisplayName()),
                buf -> buf.writeVarInt(paver.getId()));
    }

    public static int inventoryTop() {
        return TOP + ROWS * 18 + 14;
    }

    /** The settings, as the server last sent them. */
    public PaverSettings settings() {
        int[] values = new int[PaverSettings.COUNT];
        for (int i = 0; i < values.length; i++) {
            values[i] = data.get(i);
        }
        return PaverSettings.of(values);
    }

    public boolean paving() {
        return data.get(PAVING) != 0;
    }

    /** Money spent since the screed last went down, in cents. */
    public long spent() {
        return ((long) data.get(SPENT_HIGH) << 15) | (data.get(SPENT_LOW) & 0x7FFF);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        int setting = id / 2;
        if (paver == null || setting < 0 || setting >= PaverSettings.COUNT) {
            return false;
        }
        paver.setPaverSettings(paver.paverSettings().step(setting, id % 2 == 1));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int hopperEnd = ROWS * 9;
        if (index < hopperEnd) {
            if (!moveItemStackTo(stack, hopperEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, hopperEnd, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return before;
    }

    @Override
    public boolean stillValid(Player player) {
        return paver == null || (paver.isAlive() && player.distanceToSqr(paver) < 100.0);
    }
}
