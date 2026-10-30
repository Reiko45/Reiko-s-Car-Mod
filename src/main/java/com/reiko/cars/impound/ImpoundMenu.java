package com.reiko.cars.impound;

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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An impound office's screen, server side. A car owner sees their cars held here, picked with the arrows, and what it
 * takes to get each back; the lot's owner sees every car held here (and can let a towed one go) and sets the fee. The
 * car picked is kept live through data slots; actions go through {@code LotActionPayload}.
 */
public final class ImpoundMenu extends AbstractContainerMenu {
    public static final int PREVIOUS = 10;
    public static final int NEXT = 11;

    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int COLOR = 4;
    private static final int REPOSSESSED = 6;
    private static final int DAYS = 7;
    private static final int COST = 8;
    private static final int ARREARS = 12;
    private static final int SLOTS = 16;

    @Nullable
    private final ImpoundOfficeBlockEntity office; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final String ownerName;
    private final boolean manager;
    private final long fee;
    private final long earned;
    private final int impounded;
    private final int released;
    private final ContainerData data;
    @Nullable
    private CarEntity selected;

    public ImpoundMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt(),
                buf.readVarInt(), new SimpleContainerData(SLOTS));
    }

    private ImpoundMenu(int containerId, @Nullable ImpoundOfficeBlockEntity office, BlockPos pos, String ownerName, boolean manager, long fee,
                        long earned, int impounded, int released, ContainerData data) {
        super(ModMenus.IMPOUND.get(), containerId);
        this.office = office;
        this.pos = pos;
        this.ownerName = ownerName;
        this.manager = manager;
        this.fee = fee;
        this.earned = earned;
        this.impounded = impounded;
        this.released = released;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, ImpoundOfficeBlockEntity office) {
        Component title = office.ownerName().isEmpty() ? Component.translatable("container.reikocars.impound")
                : Component.translatable("container.reikocars.impound_of", office.ownerName());
        boolean manager = office.isOwner(player);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, office), title), buf -> {
            buf.writeBlockPos(office.getBlockPos());
            buf.writeUtf(office.ownerName(), 64);
            buf.writeBoolean(manager);
            buf.writeVarLong(office.fee());
            buf.writeVarLong(office.earned());
            buf.writeVarInt(office.impounded());
            buf.writeVarInt(office.released());
        });
    }

    public static ImpoundMenu create(int containerId, ServerPlayer player, ImpoundOfficeBlockEntity office) {
        ImpoundMenu menu = new ImpoundMenu(containerId, office, office.getBlockPos(), office.ownerName(), office.isOwner(player), office.fee(),
                office.earned(), office.impounded(), office.released(), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    private List<CarEntity> cars() {
        if (office == null || viewer == null) {
            return List.of();
        }
        return manager ? ImpoundService.heldAt(office) : ImpoundService.heldFor(office, viewer);
    }

    /** Finds the cars again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (office == null || viewer == null) {
            return;
        }
        List<CarEntity> cars = cars();
        if (selected == null || !cars.contains(selected)) {
            selected = cars.isEmpty() ? null : cars.get(0);
        }
        Impound held = selected == null ? null : selected.impound();
        data.set(KIND, selected == null ? 0 : 1);
        data.set(TYPE, selected == null ? 0 : selected.carType().ordinal());
        data.set(INDEX, selected == null ? 0 : cars.indexOf(selected));
        data.set(COUNT, cars.size());
        int color = selected == null ? 0 : selected.color();
        data.set(COLOR, (color >> 16) & 0xFFFF);
        data.set(COLOR + 1, color & 0xFFFF);
        data.set(REPOSSESSED, held != null && held.repossessed() ? 1 : 0);
        data.set(DAYS, held == null ? 0 : Math.min(30000, held.daysHeld(office.getLevel().getGameTime())));
        putLong(COST, selected == null ? 0 : ImpoundService.cost(office, selected));
        putLong(ARREARS, held == null ? 0 : held.arrears());
    }

    private void putLong(int slot, long value) {
        for (int i = 0; i < 4; i++) {
            data.set(slot + i, (int) ((value >> (16 * i)) & 0xFFFF));
        }
    }

    private long getLong(int slot) {
        long value = 0;
        for (int i = 0; i < 4; i++) {
            value |= ((long) (data.get(slot + i) & 0xFFFF)) << (16 * i);
        }
        return value;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == PREVIOUS || id == NEXT) {
            List<CarEntity> cars = cars();
            if (cars.isEmpty()) {
                return false;
            }
            int index = selected == null ? 0 : cars.indexOf(selected);
            selected = cars.get(Math.floorMod(index + (id == NEXT ? 1 : -1), cars.size()));
            refresh();
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public ImpoundOfficeBlockEntity office() {
        return office;
    }

    @Nullable
    public CarEntity selected() {
        return selected;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** The lot's owner: they see every car held here, and set the fee. */
    public boolean isManager() {
        return manager;
    }

    /** The release fee when the screen opened, in cents. */
    public long fee() {
        return fee;
    }

    public long earned() {
        return earned;
    }

    public int impounded() {
        return impounded;
    }

    public int released() {
        return released;
    }

    public boolean hasCar() {
        return data.get(KIND) != 0;
    }

    @Nullable
    public CarType carType() {
        int type = data.get(TYPE);
        return hasCar() && type >= 0 && type < CarType.values().length ? CarType.values()[type] : null;
    }

    public int carIndex() {
        return data.get(INDEX);
    }

    public int carCount() {
        return data.get(COUNT);
    }

    public int carColor() {
        return ((data.get(COLOR) & 0xFFFF) << 16) | (data.get(COLOR + 1) & 0xFFFF);
    }

    public boolean repossessed() {
        return data.get(REPOSSESSED) != 0;
    }

    public int daysHeld() {
        return data.get(DAYS);
    }

    /** What getting the picked car back costs, before tax: the fee and any missed loan payments. */
    public long cost() {
        return getLong(COST);
    }

    public long arrears() {
        return getLong(ARREARS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return office == null || (!office.isRemoved() && Container.stillValidBlockEntity(office, player, 8.0F));
    }
}
