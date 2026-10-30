package com.reiko.cars.traffic;

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
import org.jetbrains.annotations.Nullable;

/** A traffic light's or speed sign's owner screen: the camera's fine (and a sign's limit), and its books. */
public final class RoadSignMenu extends AbstractContainerMenu {
    @Nullable
    private final OwnedRoadsideBlockEntity sign; // null on the client
    private final BlockPos pos;
    private final boolean speedSign;
    private final long fine;
    private final int limit;
    private final boolean kmh;
    private final int tickets;
    private final long collected;

    public RoadSignMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readBoolean(), buf.readVarLong(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(),
                buf.readVarLong());
    }

    private RoadSignMenu(int containerId, @Nullable OwnedRoadsideBlockEntity sign, BlockPos pos, boolean speedSign, long fine, int limit, boolean kmh,
                         int tickets, long collected) {
        super(ModMenus.ROAD_SIGN.get(), containerId);
        this.sign = sign;
        this.pos = pos;
        this.speedSign = speedSign;
        this.fine = fine;
        this.limit = limit;
        this.kmh = kmh;
        this.tickets = tickets;
        this.collected = collected;
    }

    public static void open(ServerPlayer player, OwnedRoadsideBlockEntity sign) {
        Component title = Component.translatable(sign instanceof SpeedSignBlockEntity ? "container.reikocars.speed_sign" : "container.reikocars.traffic_light");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, sign), title), buf -> {
            buf.writeBlockPos(sign.getBlockPos());
            buf.writeBoolean(sign instanceof SpeedSignBlockEntity);
            buf.writeVarLong(sign.fine());
            buf.writeVarInt(sign instanceof SpeedSignBlockEntity speed ? speed.limit() : 0);
            buf.writeBoolean(sign instanceof SpeedSignBlockEntity speed && speed.kmh());
            buf.writeVarInt(sign.tickets());
            buf.writeVarLong(sign.collected());
        });
    }

    public static RoadSignMenu create(int containerId, OwnedRoadsideBlockEntity sign) {
        SpeedSignBlockEntity speed = sign instanceof SpeedSignBlockEntity s ? s : null;
        return new RoadSignMenu(containerId, sign, sign.getBlockPos(), speed != null, sign.fine(), speed == null ? 0 : speed.limit(),
                speed != null && speed.kmh(), sign.tickets(), sign.collected());
    }

    @Nullable
    public OwnedRoadsideBlockEntity sign() {
        return sign;
    }

    public boolean speedSign() {
        return speedSign;
    }

    public long fine() {
        return fine;
    }

    public int limit() {
        return limit;
    }

    public boolean kmh() {
        return kmh;
    }

    public int tickets() {
        return tickets;
    }

    public long collected() {
        return collected;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return sign == null || (!sign.isRemoved() && player.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
