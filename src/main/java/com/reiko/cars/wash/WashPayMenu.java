package com.reiko.cars.wash;

import com.reiko.cars.config.CarsConfig;
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

/** The pay screen at a Car Wash (a wash for the car being driven) or a Wash Bay (its time), by card or cash. */
public final class WashPayMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final int kind;
    private final long price;
    private final String ownerName;
    private final int seconds;

    public WashPayMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(), buf.readVarInt(), buf.readVarLong(), buf.readUtf(64), buf.readVarInt());
    }

    private WashPayMenu(int containerId, BlockPos pos, int kind, long price, String ownerName, int seconds) {
        super(ModMenus.WASH_PAY.get(), containerId);
        this.pos = pos;
        this.kind = kind;
        this.price = price;
        this.ownerName = ownerName;
        this.seconds = seconds;
    }

    public static void open(ServerPlayer player, WashBusinessBlockEntity wash, int kind) {
        BlockPos pos = wash.getBlockPos();
        long price = wash.price();
        String owner = wash.ownerName();
        int seconds = CarsConfig.get(CarsConfig.BAY_SECONDS);
        Component title = Component.translatable(kind == WashOwnerMenu.BAY ? "container.reikocars.wash_bay_of" : "container.reikocars.car_wash_of", owner);
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new WashPayMenu(id, pos, kind, price, owner, seconds), title), buf -> {
            buf.writeBlockPos(pos);
            buf.writeVarInt(kind);
            buf.writeVarLong(price);
            buf.writeUtf(owner, 64);
            buf.writeVarInt(seconds);
        });
    }

    public BlockPos pos() {
        return pos;
    }

    public int kind() {
        return kind;
    }

    /** The price, before tax, as it was when the screen opened. */
    public long price() {
        return price;
    }

    public String ownerName() {
        return ownerName;
    }

    /** How long a Wash Bay's price buys, in seconds. */
    public int seconds() {
        return seconds;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getCenter()) < 16.0 * 16.0;
    }
}
