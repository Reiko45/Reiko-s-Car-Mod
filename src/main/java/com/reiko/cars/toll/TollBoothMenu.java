package com.reiko.cars.toll;

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

/** A toll booth's owner screen: the toll, and what it's taken. */
public final class TollBoothMenu extends AbstractContainerMenu {
    @Nullable
    private final TollBoothBlockEntity booth; // null on the client
    private final BlockPos pos;
    private final long toll;
    private final long earned;
    private final int cars;

    public TollBoothMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
    }

    private TollBoothMenu(int containerId, @Nullable TollBoothBlockEntity booth, BlockPos pos, long toll, long earned, int cars) {
        super(ModMenus.TOLL_BOOTH.get(), containerId);
        this.booth = booth;
        this.pos = pos;
        this.toll = toll;
        this.earned = earned;
        this.cars = cars;
    }

    public static void open(ServerPlayer player, TollBoothBlockEntity booth) {
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> create(id, booth), Component.translatable("container.reikocars.toll_booth")), buf -> {
            buf.writeBlockPos(booth.getBlockPos());
            buf.writeVarLong(booth.toll());
            buf.writeVarLong(booth.earned());
            buf.writeVarInt(booth.cars());
        });
    }

    public static TollBoothMenu create(int containerId, TollBoothBlockEntity booth) {
        return new TollBoothMenu(containerId, booth, booth.getBlockPos(), booth.toll(), booth.earned(), booth.cars());
    }

    @Nullable
    public TollBoothBlockEntity booth() {
        return booth;
    }

    public long toll() {
        return toll;
    }

    public long earned() {
        return earned;
    }

    public int cars() {
        return cars;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return booth == null || (!booth.isRemoved() && player.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
