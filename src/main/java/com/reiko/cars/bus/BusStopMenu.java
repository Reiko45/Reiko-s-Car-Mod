package com.reiko.cars.bus;

import com.reiko.cars.registry.ModMenus;
import java.util.ArrayList;
import java.util.List;
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

/** A bus stop's owner screen: its name, and the routes that stop there with its number along each. */
public final class BusStopMenu extends AbstractContainerMenu {
    @Nullable
    private final BusStopBlockEntity stop; // null on the client
    private final BlockPos pos;
    private final String name;
    private final List<BusStopBlockEntity.RouteStop> routes;
    private final boolean shelter;

    public BusStopMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, null, buf.readBlockPos(), buf.readUtf(BusStopBlockEntity.MAX_NAME), readRoutes(buf), buf.readBoolean());
    }

    private BusStopMenu(int containerId, @Nullable BusStopBlockEntity stop, BlockPos pos, String name, List<BusStopBlockEntity.RouteStop> routes,
                        boolean shelter) {
        super(ModMenus.BUS_STOP.get(), containerId);
        this.stop = stop;
        this.pos = pos;
        this.name = name;
        this.routes = routes;
        this.shelter = shelter;
    }

    private static List<BusStopBlockEntity.RouteStop> readRoutes(RegistryFriendlyByteBuf buf) {
        int count = Math.min(BusStopBlockEntity.MAX_ROUTES, buf.readVarInt());
        List<BusStopBlockEntity.RouteStop> routes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            routes.add(new BusStopBlockEntity.RouteStop(buf.readVarInt(), buf.readVarInt()));
        }
        return routes;
    }

    public static void open(ServerPlayer player, BusStopBlockEntity stop) {
        boolean shelter = stop.getBlockState().getBlock() instanceof BusShelterBlock;
        Component title = Component.translatable(shelter ? "container.reikocars.bus_shelter" : "container.reikocars.bus_stop_sign");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new BusStopMenu(id, stop, stop.getBlockPos(), stop.name(), stop.routes(), shelter),
                title), buf -> {
                    buf.writeBlockPos(stop.getBlockPos());
                    buf.writeUtf(stop.name(), BusStopBlockEntity.MAX_NAME);
                    buf.writeVarInt(stop.routes().size());
                    for (BusStopBlockEntity.RouteStop route : stop.routes()) {
                        buf.writeVarInt(route.route());
                        buf.writeVarInt(route.number());
                    }
                    buf.writeBoolean(shelter);
                });
    }

    @Nullable
    public BusStopBlockEntity stop() {
        return stop;
    }

    public String name() {
        return name;
    }

    public List<BusStopBlockEntity.RouteStop> routes() {
        return routes;
    }

    public boolean shelter() {
        return shelter;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stop == null || (!stop.isRemoved() && player.distanceToSqr(pos.getCenter()) < 64.0);
    }
}
