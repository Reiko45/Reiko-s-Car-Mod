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

/** The driver's pay screen at a toll gate: the toll, by card or cash. */
public final class TollMenu extends AbstractContainerMenu {
    private final BlockPos pole;
    private final BlockPos booth;
    private final long toll;
    private final String ownerName;

    public TollMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, buf.readBlockPos(), buf.readBlockPos(), buf.readVarLong(), buf.readUtf(64));
    }

    private TollMenu(int containerId, BlockPos pole, BlockPos booth, long toll, String ownerName) {
        super(ModMenus.TOLL.get(), containerId);
        this.pole = pole;
        this.booth = booth;
        this.toll = toll;
        this.ownerName = ownerName;
    }

    public static void open(ServerPlayer driver, BlockPos pole, TollBoothBlockEntity booth) {
        BlockPos boothPos = booth.getBlockPos();
        long toll = booth.toll();
        String owner = booth.ownerName();
        driver.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TollMenu(id, pole, boothPos, toll, owner),
                Component.translatable("container.reikocars.toll", owner)), buf -> {
                    buf.writeBlockPos(pole);
                    buf.writeBlockPos(boothPos);
                    buf.writeVarLong(toll);
                    buf.writeUtf(owner, 64);
                });
    }

    public BlockPos pole() {
        return pole;
    }

    public BlockPos booth() {
        return booth;
    }

    /** The toll, before tax, as it was when the screen opened. */
    public long toll() {
        return toll;
    }

    public String ownerName() {
        return ownerName;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pole.getCenter()) < 16.0 * 16.0;
    }
}
