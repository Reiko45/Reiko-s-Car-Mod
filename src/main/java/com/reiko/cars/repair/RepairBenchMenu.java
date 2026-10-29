package com.reiko.cars.repair;

import com.reiko.cars.car.CarCondition;
import com.reiko.cars.car.CarCondition.Part;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
import com.reiko.cars.trailer.TrailerEntity;
import com.reiko.cars.trailer.TrailerType;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The repair bench screen's server side. Repairs and price changes go through {@code RepairActionPayload}; the vehicle
 * being worked on, its condition, what each job needs and what's in stock are kept live through data slots. With
 * several cars and trailers beside the bench, the screen's arrows (menu buttons {@link #PREVIOUS} and {@link #NEXT})
 * pick which. The bench's stock slots and the player's inventory are there for the owner's Stock tab, and only show
 * on it.
 */
public final class RepairBenchMenu extends AbstractContainerMenu {
    public static final int TAB_REPAIR = 0;
    public static final int TAB_STOCK = 1;
    public static final int TAB_PRICES = 2;
    /** Where the stock slots and the player's inventory sit on the Stock tab. */
    public static final int STOCK_X = 16;
    public static final int STOCK_Y = 44;
    public static final int INVENTORY_Y = 106;
    /** Menu buttons: work on the previous or next vehicle beside the bench. */
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;

    private static final int PARTS = Part.values().length;
    // Data slots (shorts): the vehicle (its kind, 1 for a car or 2 for a trailer, and type), which of how many beside
    // the bench it is, the parts it has (a bit each), then per part its condition in thousandths, the bench's
    // stock, and how many items it'd take to make the part as new.
    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int HAS_PARTS = 4;
    private static final int CONDITION = 5;
    private static final int STOCK = CONDITION + PARTS;
    private static final int NEEDED = STOCK + PARTS;
    private static final int SLOTS = NEEDED + PARTS;
    private static final int KIND_CAR = 1;
    private static final int KIND_TRAILER = 2;

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
    /** The vehicle being worked on, server side. */
    @Nullable
    private Repairable target;
    /** Picked with the arrows, rather than the bench's own choice. */
    private boolean picked;
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
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, bench), title), buf -> {
            buf.writeBlockPos(bench.getBlockPos());
            buf.writeUtf(bench.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeLongArray(prices);
            buf.writeVarLong(bench.earned());
            buf.writeVarInt(bench.repairs());
        });
    }

    /** The server side of the screen for {@code player} at {@code bench} (opened with {@link #open}). */
    public static RepairBenchMenu create(int containerId, ServerPlayer player, RepairBenchBlockEntity bench) {
        RepairBenchMenu menu = new RepairBenchMenu(containerId, player.getInventory(), bench, bench.stock(), bench.getBlockPos(),
                bench.ownerName(), bench.canManage(player), bench.isOwner(player), bench.prices(), bench.earned(), bench.repairs(),
                new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    /** Finds the vehicles beside the bench again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (bench == null || viewer == null) {
            return;
        }
        List<Repairable> vehicles = RepairService.vehiclesInReach(bench);
        if (target == null || !vehicles.contains(target) || (!picked && RepairService.defaultTarget(bench, viewer, vehicles) != target)) {
            target = RepairService.defaultTarget(bench, viewer, vehicles);
            picked = false;
        }
        CarCondition condition = target == null ? CarCondition.NEW : target.condition();
        data.set(KIND, target instanceof CarEntity ? KIND_CAR : target instanceof TrailerEntity ? KIND_TRAILER : 0);
        data.set(TYPE, target instanceof CarEntity car ? car.carType().ordinal()
                : target instanceof TrailerEntity trailer ? trailer.trailerType().ordinal() : 0);
        data.set(INDEX, target == null ? 0 : vehicles.indexOf(target));
        data.set(COUNT, vehicles.size());
        int hasParts = 0;
        for (Part part : Part.values()) {
            int i = part.ordinal();
            if (target != null && target.hasPart(part)) {
                hasParts |= 1 << i;
            }
            data.set(CONDITION + i, Math.round(condition.get(part) * 1000));
            data.set(STOCK + i, Math.min(Short.MAX_VALUE, bench.count(part)));
            data.set(NEEDED + i, target == null ? 0 : RepairJobs.needed(part, target, condition));
        }
        data.set(HAS_PARTS, hasParts);
    }

    /** The arrows: work on the previous or next vehicle beside the bench. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null || (id != PREVIOUS && id != NEXT)) {
            return false;
        }
        List<Repairable> vehicles = RepairService.vehiclesInReach(bench);
        if (vehicles.isEmpty()) {
            return false;
        }
        int index = target == null ? -1 : vehicles.indexOf(target);
        int step = id == NEXT ? 1 : -1;
        target = vehicles.get(Math.floorMod(index < 0 ? 0 : index + step, vehicles.size()));
        picked = true;
        refresh();
        return true;
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

    /** The vehicle being worked on, server side. */
    @Nullable
    public Repairable target() {
        return target;
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

    /** Whether there's a vehicle to work on. */
    public boolean hasTarget() {
        return data.get(KIND) != 0;
    }

    /** The kind of vehicle being worked on, like "Sedan" or "Box Trailer". */
    public Component targetName() {
        int type = data.get(TYPE);
        if (data.get(KIND) == KIND_CAR && type >= 0 && type < CarType.values().length) {
            return Component.translatable("entity.reikocars." + CarType.values()[type].id());
        }
        if (data.get(KIND) == KIND_TRAILER && type >= 0 && type < TrailerType.values().length) {
            return Component.translatable("entity.reikocars." + TrailerType.values()[type].id());
        }
        return Component.empty();
    }

    /** Which of the vehicles beside the bench is being worked on (from 0), and how many there are. */
    public int targetIndex() {
        return data.get(INDEX);
    }

    public int vehicleCount() {
        return data.get(COUNT);
    }

    /** Whether the vehicle has {@code part} (a trailer has no oil or engine). */
    public boolean hasPart(Part part) {
        return (data.get(HAS_PARTS) & (1 << part.ordinal())) != 0;
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
