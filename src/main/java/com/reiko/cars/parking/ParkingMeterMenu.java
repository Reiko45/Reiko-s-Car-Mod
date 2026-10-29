package com.reiko.cars.parking;

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
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A parking meter's screen, server side: the car parked at it and the paid time it has left, kept live in data slots,
 * and the meter's rate and fine. Paying and the owner's settings go through {@code MeterActionPayload}.
 */
public final class ParkingMeterMenu extends AbstractContainerMenu {
    private static final int HAS_CAR = 0;
    private static final int TYPE = 1;
    private static final int SECONDS = 2; // two slots
    private static final int EXPIRED = 4;
    private static final int SLOTS = 5;

    @Nullable
    private final ParkingMeterBlockEntity meter; // null on the client
    private final BlockPos pos;
    private final String ownerName;
    private final boolean owner;
    private final long rate;
    private final long fine;
    private final long earned;
    private final long fined;
    private final int tickets;
    private final ContainerData data;

    public ParkingMeterMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(64), buf.readBoolean(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarInt(), new SimpleContainerData(SLOTS));
    }

    private ParkingMeterMenu(int containerId, @Nullable ParkingMeterBlockEntity meter, BlockPos pos, String ownerName, boolean owner,
                             long rate, long fine, long earned, long fined, int tickets, ContainerData data) {
        super(ModMenus.PARKING_METER.get(), containerId);
        this.meter = meter;
        this.pos = pos;
        this.ownerName = ownerName;
        this.owner = owner;
        this.rate = rate;
        this.fine = fine;
        this.earned = earned;
        this.fined = fined;
        this.tickets = tickets;
        this.data = data;
        addDataSlots(data);
    }

    public static void open(ServerPlayer player, ParkingMeterBlockEntity meter) {
        Component title = meter.ownerName().isEmpty()
                ? Component.translatable("container.reikocars.parking_meter")
                : Component.translatable("container.reikocars.parking_meter_of", meter.ownerName());
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, player, meter), title), buf -> {
            buf.writeBlockPos(meter.getBlockPos());
            buf.writeUtf(meter.ownerName(), 64);
            buf.writeBoolean(meter.isOwner(player));
            buf.writeVarLong(meter.rate());
            buf.writeVarLong(meter.fine());
            buf.writeVarLong(meter.earned());
            buf.writeVarLong(meter.fined());
            buf.writeVarInt(meter.tickets());
        });
    }

    public static ParkingMeterMenu create(int containerId, ServerPlayer player, ParkingMeterBlockEntity meter) {
        ParkingMeterMenu menu = new ParkingMeterMenu(containerId, meter, meter.getBlockPos(), meter.ownerName(), meter.isOwner(player),
                meter.rate(), meter.fine(), meter.earned(), meter.fined(), meter.tickets(), new SimpleContainerData(SLOTS));
        menu.refresh();
        return menu;
    }

    public void refresh() {
        if (meter == null || meter.getLevel() == null) {
            return;
        }
        CarEntity car = MeterService.parkedAt(meter);
        data.set(HAS_CAR, car == null ? 0 : 1);
        data.set(TYPE, car == null ? 0 : car.carType().ordinal());
        int seconds = (int) Math.min(Integer.MAX_VALUE, meter.timeLeft(car, meter.getLevel().getGameTime()) / 20);
        data.set(SECONDS, (seconds >> 16) & 0xFFFF);
        data.set(SECONDS + 1, seconds & 0xFFFF);
        data.set(EXPIRED, meter.isExpired() ? 1 : 0);
    }

    @Override
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Nullable
    public ParkingMeterBlockEntity meter() {
        return meter;
    }

    public BlockPos pos() {
        return pos;
    }

    public String ownerName() {
        return ownerName;
    }

    /** The meter's owner: they get the Settings tab, and park free. */
    public boolean isOwner() {
        return owner;
    }

    public long rate() {
        return rate;
    }

    public long fine() {
        return fine;
    }

    public long earned() {
        return earned;
    }

    public long fined() {
        return fined;
    }

    public int tickets() {
        return tickets;
    }

    public boolean hasCar() {
        return data.get(HAS_CAR) != 0;
    }

    @Nullable
    public CarType carType() {
        int type = data.get(TYPE);
        return hasCar() && type >= 0 && type < CarType.values().length ? CarType.values()[type] : null;
    }

    /** Paid time left for the car parked here, in seconds. */
    public int secondsLeft() {
        return ((data.get(SECONDS) & 0xFFFF) << 16) | (data.get(SECONDS + 1) & 0xFFFF);
    }

    /** Whether the red flag's up: the car's overstayed. */
    public boolean expired() {
        return data.get(EXPIRED) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return meter == null || (!meter.isRemoved() && Container.stillValidBlockEntity(meter, player, 8.0F));
    }
}
