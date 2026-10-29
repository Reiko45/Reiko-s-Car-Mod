package com.reiko.cars.insurance;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.config.CarsConfig;
import com.reiko.cars.registry.ModMenus;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * An insurance office's screen, server side. A customer sees their cars parked nearby (picked with the arrows), each
 * one's cover, and the office's terms, and buys cover; the owner sets the terms and sees the books. The picked car
 * is kept live through data slots; actions go through {@code InsuranceActionPayload}.
 */
public final class InsuranceMenu extends AbstractContainerMenu {
    /** Menu buttons: pick the previous or next car. */
    public static final int PREVIOUS = 0;
    public static final int NEXT = 1;
    /** A car's cover, as the screen shows it. */
    public static final int UNINSURED = 0;
    public static final int INSURED_HERE = 1;
    public static final int INSURED_ELSEWHERE = 2;
    public static final int LAPSED = 3;

    // Data slots (16 bits each): whether there's a car picked, its type, which of how many it is, its colour (two
    // slots), its cover and the whole days left on it.
    private static final int KIND = 0;
    private static final int TYPE = 1;
    private static final int INDEX = 2;
    private static final int COUNT = 3;
    private static final int COLOR = 4;
    private static final int STATUS = 6;
    private static final int DAYS = 7;
    private static final int SLOTS = 8;

    @Nullable
    private final InsuranceOfficeBlockEntity office; // null on the client
    @Nullable
    private ServerPlayer viewer;
    private final BlockPos pos;
    private final String ownerName;
    private final boolean owner;
    private final long premium;
    private final long deductible;
    private final long limit;
    private final int termDays;
    private final long premiums;
    private final int policies;
    private final long paidOut;
    private final int claims;
    private final ContainerData data;
    @Nullable
    private CarEntity selected;

    public InsuranceMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(), buf.readVarLong(), buf.readVarInt(),
                new SimpleContainerData(SLOTS));
    }

    private InsuranceMenu(int containerId, @Nullable InsuranceOfficeBlockEntity office, BlockPos pos, String ownerName, boolean owner,
                          long premium, long deductible, long limit, int termDays, long premiums, int policies, long paidOut, int claims,
                          ContainerData data) {
        super(ModMenus.INSURANCE.get(), containerId);
        this.office = office;
        this.pos = pos;
        this.ownerName = ownerName;
        this.owner = owner;
        this.premium = premium;
        this.deductible = deductible;
        this.limit = limit;
        this.termDays = termDays;
        this.premiums = premiums;
        this.policies = policies;
        this.paidOut = paidOut;
        this.claims = claims;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, InsuranceOfficeBlockEntity office) {
        Component title = office.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.insurance_office")
                : Component.translatable("container.reikocars.insurance_office_of", office.ownerName());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, office), title), buf -> {
            buf.writeBlockPos(office.getBlockPos());
            buf.writeUtf(office.ownerName(), 64);
            buf.writeBoolean(office.isOwner(player));
            buf.writeVarLong(office.premium());
            buf.writeVarLong(office.deductible());
            buf.writeVarLong(office.limit());
            buf.writeVarInt(CarsConfig.get(CarsConfig.INSURANCE_TERM_DAYS));
            buf.writeVarLong(office.premiums());
            buf.writeVarInt(office.policies());
            buf.writeVarLong(office.paidOut());
            buf.writeVarInt(office.claims());
        });
    }

    public static InsuranceMenu create(int containerId, ServerPlayer player, InsuranceOfficeBlockEntity office) {
        InsuranceMenu menu = new InsuranceMenu(containerId, office, office.getBlockPos(), office.ownerName(), office.isOwner(player),
                office.premium(), office.deductible(), office.limit(), CarsConfig.get(CarsConfig.INSURANCE_TERM_DAYS), office.premiums(),
                office.policies(), office.paidOut(), office.claims(), new SimpleContainerData(SLOTS));
        menu.viewer = player;
        menu.refresh();
        return menu;
    }

    private List<CarEntity> cars() {
        return office == null || viewer == null || owner ? List.of() : InsuranceService.carsAt(office, viewer);
    }

    /** Finds the customer's cars again and updates the data slots. Server side, every tick. */
    public void refresh() {
        if (office == null || viewer == null) {
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
        Policy active = selected == null ? null : selected.activePolicy();
        Policy any = selected == null ? null : selected.policy();
        data.set(STATUS, active != null ? (active.insurer().equals(office.owner()) ? INSURED_HERE : INSURED_ELSEWHERE)
                : any != null && selected.isOwner(viewer) ? LAPSED : UNINSURED);
        data.set(DAYS, active == null ? 0 : Math.min(Short.MAX_VALUE, active.daysLeft(viewer.serverLevel().getGameTime())));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != PREVIOUS && id != NEXT) {
            return false;
        }
        List<CarEntity> cars = cars();
        if (cars.isEmpty()) {
            return false;
        }
        int index = selected == null ? 0 : cars.indexOf(selected);
        selected = cars.get(Math.floorMod(index + (id == NEXT ? 1 : -1), cars.size()));
        refresh();
        return true;
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public InsuranceOfficeBlockEntity office() {
        return office;
    }

    /** The car picked, server side. */
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

    /** The office's owner, who gets the settings instead of the cars. */
    public boolean isOwner() {
        return owner;
    }

    public long premium() {
        return premium;
    }

    public long deductible() {
        return deductible;
    }

    public long limit() {
        return limit;
    }

    public int termDays() {
        return termDays;
    }

    public long premiums() {
        return premiums;
    }

    public int policies() {
        return policies;
    }

    public long paidOut() {
        return paidOut;
    }

    public int claims() {
        return claims;
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

    /** {@link #UNINSURED}, {@link #INSURED_HERE}, {@link #INSURED_ELSEWHERE} or {@link #LAPSED}. */
    public int status() {
        return data.get(STATUS);
    }

    public int daysLeft() {
        return data.get(DAYS);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return office == null || (!office.isRemoved() && player.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
