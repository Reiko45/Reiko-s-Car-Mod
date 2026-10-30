package com.reiko.cars.wash;

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

/** A Car Wash's or Wash Bay's owner screen: its price, and what it's taken. */
public final class WashOwnerMenu extends AbstractContainerMenu {
    /** Which kind of business: the drive-through Car Wash, or a Wash Bay. */
    public static final int TUNNEL = 0;
    public static final int BAY = 1;

    @Nullable
    private final WashBusinessBlockEntity wash; // null on the client
    private final BlockPos pos;
    private final int kind;
    private final long price;
    private final long earned;
    private final int washes;

    public WashOwnerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
    }

    private WashOwnerMenu(int containerId, @Nullable WashBusinessBlockEntity wash, BlockPos pos, int kind, long price, long earned, int washes) {
        super(ModMenus.WASH_OWNER.get(), containerId);
        this.wash = wash;
        this.pos = pos;
        this.kind = kind;
        this.price = price;
        this.earned = earned;
        this.washes = washes;
    }

    public static void open(ServerPlayer player, WashBusinessBlockEntity wash, int kind) {
        Component title = Component.translatable(kind == BAY ? "container.reikocars.wash_bay" : "container.reikocars.car_wash");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WashOwnerMenu(id, wash, wash.getBlockPos(), kind, wash.price(), wash.earned(),
                wash.washes()), title), buf -> {
                    buf.writeBlockPos(wash.getBlockPos());
                    buf.writeVarInt(kind);
                    buf.writeVarLong(wash.price());
                    buf.writeVarLong(wash.earned());
                    buf.writeVarInt(wash.washes());
                });
    }

    @Nullable
    public WashBusinessBlockEntity wash() {
        return wash;
    }

    public int kind() {
        return kind;
    }

    public long price() {
        return price;
    }

    public long earned() {
        return earned;
    }

    public int washes() {
        return washes;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return wash == null || (!wash.isRemoved() && player.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
