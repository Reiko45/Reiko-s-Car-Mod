package com.reiko.cars.dmv;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.license.DriverLicense;
import com.reiko.cars.license.LicenseService;
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
 * A DMV's screen, server side. Everyone: their license (and the driving test, or a replacement card) and plates for
 * their cars parked nearby (picked with the arrows, kept live through data slots). The owner: the fees and the books
 * too. What the screen shows about the license, the fees and the route is sent when it opens; it's reopened after
 * anything that changes them. Actions go through {@code LotActionPayload}.
 */
public final class DmvMenu extends AbstractContainerMenu {
    public static final int TAB_LICENSE = 0;
    public static final int TAB_PLATES = 1;
    public static final int TAB_SETTINGS = 2;
    public static final int PREVIOUS = 10;
    public static final int NEXT = 11;

    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int COLOR = 4;
    private static final int ENTITY = 6;
    private static final int SLOTS = 8;

    /** What the screen knows of the DMV and the viewer's license, from when it opened. */
    public record Info(String ownerName, boolean manager, long[] fees, long earned, int tests, int passes, int plates, int routeCones,
                       int testMinutes, LicenseService.Status status, String number, int points, int maxPoints, long suspendedLeft,
                       boolean onTest) {
    }

    @Nullable
    private final DmvOfficeBlockEntity dmv; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final Info info;
    private final ContainerData data;
    @Nullable
    private CarEntity selected;

    public DmvMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), read(buf), new SimpleContainerData(SLOTS));
    }

    private DmvMenu(int containerId, @Nullable DmvOfficeBlockEntity dmv, BlockPos pos, Info info, ContainerData data) {
        super(ModMenus.DMV.get(), containerId);
        this.dmv = dmv;
        this.pos = pos;
        this.info = info;
        this.data = data;
        addDataSlots(data);
    }

    private static Info read(RegistryFriendlyByteBuf buf) {
        String owner = buf.readUtf(64);
        boolean manager = buf.readBoolean();
        long[] fees = {buf.readVarLong(), buf.readVarLong(), buf.readVarLong()};
        return new Info(owner, manager, fees, buf.readVarLong(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), LicenseService.Status.values()[buf.readVarInt()], buf.readUtf(32), buf.readVarInt(), buf.readVarInt(),
                buf.readVarLong(), buf.readBoolean());
    }

    private static void write(RegistryFriendlyByteBuf buf, Info info) {
        buf.writeUtf(info.ownerName(), 64);
        buf.writeBoolean(info.manager());
        for (long fee : info.fees()) {
            buf.writeVarLong(fee);
        }
        buf.writeVarLong(info.earned());
        buf.writeVarInt(info.tests());
        buf.writeVarInt(info.passes());
        buf.writeVarInt(info.plates());
        buf.writeVarInt(info.routeCones());
        buf.writeVarInt(info.testMinutes());
        buf.writeVarInt(info.status().ordinal());
        buf.writeUtf(info.number(), 32);
        buf.writeVarInt(info.points());
        buf.writeVarInt(info.maxPoints());
        buf.writeVarLong(info.suspendedLeft());
        buf.writeBoolean(info.onTest());
    }

    /** What {@code player} sees at {@code dmv}: the fees they'd pay (nothing at their own), their license, the route. */
    private static Info infoFor(ServerPlayer player, DmvOfficeBlockEntity dmv) {
        DriverLicense license = LicenseService.of(player.server, player.getUUID());
        long now = LicenseService.now(player.server);
        long[] fees = {DmvService.price(dmv, player, 0), DmvService.price(dmv, player, 1), DmvService.price(dmv, player, 2)};
        if (dmv.isOwner(player)) {
            fees = new long[] {dmv.fee(0), dmv.fee(1), dmv.fee(2)}; // the owner's screen shows what customers pay
        }
        return new Info(dmv.ownerName(), dmv.isOwner(player), fees, dmv.earned(), dmv.tests(), dmv.passes(), dmv.platesSold(),
                DmvService.route(dmv).size(), com.reiko.cars.config.CarsConfig.get(com.reiko.cars.config.CarsConfig.TEST_MINUTES),
                LicenseService.status(player.server, player.getUUID()), license == null ? "" : license.number(),
                license == null ? 0 : LicenseService.points(player.server, license), LicenseService.maxPoints(),
                license == null ? 0 : Math.max(0, license.suspendedUntil() - now), DrivingTests.taking(player));
    }

    public static void open(ServerPlayer player, DmvOfficeBlockEntity dmv) {
        Component title = dmv.ownerName().isEmpty() ? Component.translatable("container.reikocars.dmv")
                : Component.translatable("container.reikocars.dmv_of", dmv.ownerName());
        Info info = infoFor(player, dmv);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, dmv), title), buf -> {
            buf.writeBlockPos(dmv.getBlockPos());
            write(buf, info);
        });
    }

    public static DmvMenu create(int containerId, ServerPlayer player, DmvOfficeBlockEntity dmv) {
        DmvMenu menu = new DmvMenu(containerId, dmv, dmv.getBlockPos(), infoFor(player, dmv), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    private List<CarEntity> cars() {
        return dmv == null || viewer == null ? List.of() : DmvService.cars(dmv, viewer);
    }

    /** Finds the viewer's cars again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (dmv == null || viewer == null) {
            return;
        }
        List<CarEntity> cars = cars();
        if (selected == null || !cars.contains(selected)) {
            selected = cars.isEmpty() ? null : cars.get(0);
        }
        data.set(KIND, selected == null ? 0 : 1);
        data.set(TYPE, selected == null ? 0 : selected.carType().ordinal());
        data.set(INDEX, selected == null ? 0 : cars.indexOf(selected));
        data.set(COUNT, cars.size());
        int color = selected == null ? 0 : selected.color();
        data.set(COLOR, (color >> 16) & 0xFFFF);
        data.set(COLOR + 1, color & 0xFFFF);
        int id = selected == null ? -1 : selected.getId();
        data.set(ENTITY, (id >> 16) & 0xFFFF);
        data.set(ENTITY + 1, id & 0xFFFF);
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
    public DmvOfficeBlockEntity dmv() {
        return dmv;
    }

    @Nullable
    public CarEntity selected() {
        return selected;
    }

    public BlockPos pos() {
        return pos;
    }

    public Info info() {
        return info;
    }

    public boolean isManager() {
        return info.manager();
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

    /** The picked car's entity id (for its plate, which the client knows), or -1. */
    public int carEntityId() {
        return ((data.get(ENTITY) & 0xFFFF) << 16) | (data.get(ENTITY + 1) & 0xFFFF);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return dmv == null || (!dmv.isRemoved() && Container.stillValidBlockEntity(dmv, player, 8.0F));
    }
}
