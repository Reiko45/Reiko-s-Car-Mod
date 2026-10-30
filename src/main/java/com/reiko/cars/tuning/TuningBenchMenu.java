package com.reiko.cars.tuning;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModMenus;
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
 * The Tuning Bench screen's server side. The car being worked on (picked with the arrows from those beside the
 * bench), its upgrades and livery, whether the viewer may tune it, and what's in stock are kept live through data
 * slots; actions go through {@code TuningActionPayload}. The stock slots and the player's inventory show only on the
 * owner's Stock tab.
 */
public final class TuningBenchMenu extends AbstractContainerMenu {
    public static final int TAB_TUNE = 0;
    public static final int TAB_LIVERY = 1;
    public static final int TAB_STOCK = 2;
    public static final int TAB_PRICES = 3;
    public static final int STOCK_X = 16;
    public static final int STOCK_Y = 44;
    public static final int INVENTORY_Y = 106;
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;

    private static final int KITS = TuningKit.values().length;
    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int UPGRADES = 4;
    private static final int LIVERY = 5;
    private static final int MAY_TUNE = 6;
    private static final int STOCK = 7;
    private static final int SLOTS = STOCK + KITS;

    @Nullable
    private final TuningBenchBlockEntity bench; // null on the client
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final boolean owner;
    private final long[] prices;
    private final long earned;
    private final int jobs;
    private final ContainerData data;
    @Nullable
    private ServerPlayer viewer;
    @Nullable
    private CarEntity target;
    private boolean picked;
    private int tab = TAB_TUNE;

    public TuningBenchMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, null, clientStock(), buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readBoolean(),
                buf.readLongArray(), buf.readVarLong(), buf.readVarInt(), new SimpleContainerData(SLOTS));
    }

    private TuningBenchMenu(int containerId, Inventory inventory, @Nullable TuningBenchBlockEntity bench, IItemHandler stock, BlockPos pos,
                            String ownerName, boolean manager, boolean owner, long[] prices, long earned, int jobs, ContainerData data) {
        super(ModMenus.TUNING_BENCH.get(), containerId);
        this.bench = bench;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.owner = owner;
        this.prices = prices.length == KITS ? prices : new long[KITS];
        this.earned = earned;
        this.jobs = jobs;
        this.data = data;
        for (int i = 0; i < TuningBenchBlockEntity.STOCK_SLOTS; i++) {
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

    private static IItemHandler clientStock() {
        return new ItemStackHandler(TuningBenchBlockEntity.STOCK_SLOTS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TuningService.kitOf(stack) != null;
            }
        };
    }

    public static void open(ServerPlayer player, TuningBenchBlockEntity bench) {
        Component title = bench.ownerName().isEmpty() ? Component.translatable("container.reikocars.tuning_bench")
                : Component.translatable("container.reikocars.tuning_bench_of", bench.ownerName());
        boolean manager = bench.canManage(player);
        boolean owner = bench.isOwner(player);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, bench), title), buf -> {
            buf.writeBlockPos(bench.getBlockPos());
            buf.writeUtf(bench.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeBoolean(owner);
            buf.writeLongArray(bench.prices());
            buf.writeVarLong(bench.earned());
            buf.writeVarInt(bench.jobs());
        });
    }

    public static TuningBenchMenu create(int containerId, ServerPlayer player, TuningBenchBlockEntity bench) {
        TuningBenchMenu menu = new TuningBenchMenu(containerId, player.getInventory(), bench, bench.stock(), bench.getBlockPos(), bench.ownerName(),
                bench.canManage(player), bench.isOwner(player), bench.prices(), bench.earned(), bench.jobs(), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    /** The car {@code viewer} would work on, until they pick another: the one they're in, else their own nearest, else the nearest. */
    @Nullable
    private CarEntity defaultTarget(List<CarEntity> cars) {
        if (viewer != null && viewer.getVehicle() instanceof CarEntity riding && cars.contains(riding)) {
            return riding;
        }
        for (CarEntity car : cars) {
            if (viewer != null && car.isOwner(viewer)) {
                return car;
            }
        }
        return cars.isEmpty() ? null : cars.get(0);
    }

    /** Finds the cars beside the bench again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (bench == null || viewer == null) {
            return;
        }
        List<CarEntity> cars = TuningService.carsInReach(bench);
        if (target == null || !cars.contains(target) || (!picked && defaultTarget(cars) != target)) {
            target = defaultTarget(cars);
            picked = false;
        }
        data.set(KIND, target == null ? 0 : 1);
        data.set(TYPE, target == null ? 0 : target.carType().ordinal());
        data.set(INDEX, target == null ? 0 : cars.indexOf(target));
        data.set(COUNT, cars.size());
        data.set(UPGRADES, target == null ? 0 : target.upgrades().pack());
        data.set(LIVERY, target == null ? 0 : target.livery().pack());
        data.set(MAY_TUNE, target != null && TuningService.mayTune(viewer, target) ? 1 : 0);
        for (TuningKit kit : TuningKit.values()) {
            data.set(STOCK + kit.ordinal(), Math.min(Short.MAX_VALUE, bench.count(kit)));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bench == null || (id != PREVIOUS && id != NEXT)) {
            return false;
        }
        List<CarEntity> cars = TuningService.carsInReach(bench);
        if (cars.isEmpty()) {
            return false;
        }
        int index = target == null ? -1 : cars.indexOf(target);
        target = cars.get(Math.floorMod(index < 0 ? 0 : index + (id == NEXT ? 1 : -1), cars.size()));
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
    public TuningBenchBlockEntity bench() {
        return bench;
    }

    @Nullable
    public CarEntity target() {
        return target;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    public boolean isManager() {
        return manager;
    }

    /** The viewer owns the bench: kits from its stock are free to them. */
    public boolean isOwner() {
        return owner;
    }

    public long price(TuningKit kit) {
        return prices[kit.ordinal()];
    }

    public long earned() {
        return earned;
    }

    public int jobs() {
        return jobs;
    }

    public boolean hasTarget() {
        return data.get(KIND) != 0;
    }

    @Nullable
    public CarType carType() {
        int type = data.get(TYPE);
        return hasTarget() && type >= 0 && type < CarType.values().length ? CarType.values()[type] : null;
    }

    public int targetIndex() {
        return data.get(INDEX);
    }

    public int carCount() {
        return data.get(COUNT);
    }

    public Upgrades upgrades() {
        return Upgrades.unpack(data.get(UPGRADES) & 0xFFFF);
    }

    public Livery livery() {
        return Livery.unpack(data.get(LIVERY) & 0xFFFF);
    }

    /** Whether the viewer may have the car tuned (theirs, or nobody's). */
    public boolean mayTune() {
        return data.get(MAY_TUNE) != 0;
    }

    public int stock(TuningKit kit) {
        return data.get(STOCK + kit.ordinal());
    }

    /** Client: the tab showing, which decides whether the slots do. */
    public void setTab(int tab) {
        this.tab = tab;
    }

    private boolean stockShown() {
        return manager && tab == TAB_STOCK;
    }

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
        int stockEnd = TuningBenchBlockEntity.STOCK_SLOTS;
        if (index < stockEnd) {
            if (!moveItemStackTo(stack, stockEnd, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TuningService.kitOf(stack) == null || !moveItemStackTo(stack, 0, stockEnd, false)) {
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
