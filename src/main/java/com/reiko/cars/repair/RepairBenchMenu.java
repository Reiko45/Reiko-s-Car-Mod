package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The repair bench screen's server side. Repairs and price changes go through {@code RepairActionPayload}; the car
 * beside the bench, its condition, what each job needs and what's in stock are kept live through data slots. The
 * bench's stock slots and the player's inventory are there for the owner's Stock tab, and only show on it.
 */
public final class RepairBenchMenu extends AbstractContainerMenu {
    public static final int TAB_REPAIR = 0;
    public static final int TAB_STOCK = 1;
    public static final int TAB_PRICES = 2;
    /** Where the stock slots and the player's inventory sit on the Stock tab. */
    public static final int STOCK_X = 16;
    public static final int STOCK_Y = 44;
    public static final int INVENTORY_Y = 106;

    private static final int PARTS = Part.values().length;
    // Data slots (shorts): the car's type, then per part its condition in thousandths, the bench's stock, and how
    // many items it'd take to make the part as new.
    private static final int CAR_TYPE = 0;
    private static final int CONDITION = 1;
    private static final int STOCK = CONDITION + PARTS;
    private static final int NEEDED = STOCK + PARTS;
    private static final int SLOTS = NEEDED + PARTS;

    @Nullable
    private final RepairBenchBlockEntity bench; // null on the client
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final boolean owner;
    private final long[] prices;
    private final long earned;
    private final int repairs;
    private final ContainerData data;
    @Nullable
    private ServerPlayer viewer;
    @Nullable
    private CarEntity car;
    /** Client: the tab the screen shows, which decides whether the slots do. */
    private int tab = TAB_REPAIR;

    public RepairBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, clientStock(), buf.readBlockPos(), buf.readUtf(64),
                buf.readBoolean(), buf.readBoolean(), buf.readLongArray(), buf.readVarLong(), buf.readVarInt(), new SimpleContainerData(SLOTS));
    }

    private RepairBenchMenu(int containerId, Inventory inventory, @Nullable RepairBenchBlockEntity bench, IItemHandler stock, BlockPos pos,
                            String ownerName, boolean manager, boolean owner, long[] prices, long earned, int repairs, ContainerData data) {
        super(ModMenus.REPAIR_BENCH.get(), containerId);
        this.bench = bench;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.owner = owner;
        this.prices = prices.length == PARTS ? prices : new long[PARTS];
        this.earned = earned;
        this.repairs = repairs;
        this.data = data;
        for (int i = 0; i < RepairBenchBlockEntity.STOCK_SLOTS; i++) {
            addSlot(new SlotItemHandler(stock, i, STOCK_X + (i % 9) * 18, STOCK_Y + (i / 9) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return manager && super.mayPlace(stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return manager && super.mayPickup(player);
                }

                @Override
                public boolean isActive() {
                    return stockShown();
                }
            });
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int index = row == 3 ? col : col + (row + 1) * 9;
                int y = row == 3 ? INVENTORY_Y + 58 : INVENTORY_Y + row * 18;
                addSlot(new Slot(inventory, index, STOCK_X + col * 18, y) {
                    @Override
                    public boolean isActive() {
                        return stockShown();
                    }
                });
            }
        }
        addDataSlots(data);
    }

    /** The client's copy of the stock, filled in by the server; it takes only parts, as the bench does. */
    private static IItemHandler clientStock() {
        return new ItemStackHandler(RepairBenchBlockEntity.STOCK_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return RepairJobs.partFor(stack) != null;
            }
        };
    }

    public static void open(ServerPlayer player, RepairBenchBlockEntity bench) {
        Component title = bench.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.repair_bench")
                : Component.translatable("container.reikocars.repair_bench_of", bench.ownerName());
        boolean manager = bench.canManage(player);
        boolean owner = bench.isOwner(player);
        long[] prices = bench.prices();
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
            RepairBenchMenu menu = new RepairBenchMenu(id, inventory, bench, bench.stock(), bench.getBlockPos(), bench.ownerName(), manager,
                    owner, prices, bench.earned(), bench.repairs(), new SimpleContainerData(SLOTS));
            menu.viewer = player;
            menu.refresh();
            return menu;
        }, title), buf -> {
            buf.writeBlockPos(bench.getBlockPos());
            buf.writeUtf(bench.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeLongArray(prices);
            buf.writeVarLong(bench.earned());
            buf.writeVarInt(bench.repairs());
        });
    }

    /** Finds the car beside the bench again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (bench == null || viewer == null) {
            return;
        }
        if (car == null || !RepairService.inReach(bench, car) || !car.canBeUsedBy(viewer)
                || (viewer.getVehicle() instanceof CarEntity riding && riding != car)) {
            car = RepairService.findCar(bench, viewer);
        }
        CarCondition condition = car == null ? CarCondition.NEW : car.condition();
        data.set(CAR_TYPE, car == null ? -1 : car.carType().ordinal());
        for (Part part : Part.values()) {
            int i = part.ordinal();
            data.set(CONDITION + i, Math.round(condition.get(part) * 1000));
            data.set(STOCK + i, Math.min(Short.MAX_VALUE, bench.count(part)));
            data.set(NEEDED + i, car == null ? 0 : RepairJobs.needed(part, car.carType(), condition));
        }
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public RepairBenchBlockEntity bench() {
        return bench;
    }

    /** The car beside the bench, server side. */
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

    /** Whether the viewer owns the bench (or is an admin): they can stock it and set its prices. */
    public boolean isManager() {
        return manager;
    }

    /** Whether the viewer owns the bench, and so repairs from its stock for free. */
    public boolean isOwner() {
        return owner;
    }

    /** Cents per item of {@code part}, as it was when the screen opened. */
    public long price(Part part) {
        return prices[part.ordinal()];
    }

    public long earned() {
        return earned;
    }

    public int repairs() {
        return repairs;
    }

    @Nullable
    public CarType carType() {
        int ordinal = data.get(CAR_TYPE);
        return ordinal >= 0 && ordinal < CarType.values().length ? CarType.values()[ordinal] : null;
    }

    /** The car's condition, to a thousandth. */
    public CarCondition condition() {
        float[] v = new float[PARTS];
        for (int i = 0; i < PARTS; i++) {
            v[i] = data.get(CONDITION + i) / 1000.0F;
        }
        return new CarCondition(v[0], v[1], v[2], v[3], v[4]);
    }

    /** Items of {@code part} in the bench's stock. */
    public int stock(Part part) {
        return data.get(STOCK + part.ordinal());
    }

    /** Items of {@code part} it'd take to make the car's part as new: 0 if it's fine. */
    public int needed(Part part) {
        return data.get(NEEDED + part.ordinal());
    }

    /** Client: the tab showing on the screen. */
    public void setTab(int tab) {
        this.tab = tab;
    }

    private boolean stockShown() {
        return manager && tab == TAB_STOCK;
    }

    /** Shift-click moves parts between the player's inventory and the bench's stock, for the owner. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (!manager) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int stockEnd = RepairBenchBlockEntity.STOCK_SLOTS;
        if (index < stockEnd) {
            if (!moveItemStackTo(stack, stockEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (RepairJobs.partFor(stack) == null || !moveItemStackTo(stack, 0, stockEnd, false)) {
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
        return bench == null || (!bench.isRemoved() && Container.stillValidBlockEntity(bench, player, 8.0F));
    }
}
