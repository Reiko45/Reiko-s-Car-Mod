package com.reiko.cars.bus;

import com.reiko.cars.registry.ModBlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A bus stop (a sign, or the main block of a shelter): whose it is, its name, and the routes that stop there, each
 * with the stop's number along it. A bus on one of its owner's routes stops at the route's stops in number order.
 */
public final class BusStopBlockEntity extends BlockEntity {
    /** How many routes one stop can be on. */
    public static final int MAX_ROUTES = 3;
    /** The longest name a stop can have. */
    public static final int MAX_NAME = 24;

    /** A route the stop is on, and its number along it (1 is the first stop). */
    public record RouteStop(int route, int number) {
    }

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private String name = "";
    private List<RouteStop> routes = List.of();

    public BusStopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUS_STOP.get(), pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public void setOwner(Player player) {
        owner = player.getUUID();
        ownerName = player.getGameProfile().getName();
        changed();
    }

    public boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** The owner or an admin may name it, set its routes and break it. Anyone may if nobody owns it. */
    public boolean canManage(Player player) {
        return owner == null || isOwner(player) || player.hasPermissions(2);
    }

    /** Its name as set ("" if none). */
    public String name() {
        return name;
    }

    /** Its name, or "Bus stop" if it hasn't one. */
    public Component displayName() {
        return name.isEmpty() ? Component.translatable("bus.reikocars.unnamed_stop") : Component.literal(name);
    }

    public List<RouteStop> routes() {
        return routes;
    }

    /** Its number along {@code route}, or 0 if it isn't on it. */
    public int numberOn(int route) {
        for (RouteStop stop : routes) {
            if (stop.route() == route) {
                return stop.number();
            }
        }
        return 0;
    }

    /** "1, 4": the routes that stop here, in order. */
    public String routesText() {
        return BusStopRegistry.routesText(routes);
    }

    public void configure(String newName, List<RouteStop> newRoutes) {
        name = newName;
        routes = List.copyOf(newRoutes);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (level instanceof ServerLevel server) {
                BusStopRegistry.get(server.getServer()).update(this);
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            BusStopRegistry.get(server.getServer()).update(this);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    private void write(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        tag.putString("OwnerName", ownerName);
        tag.putString("Name", name);
        int[] packed = new int[routes.size() * 2];
        for (int i = 0; i < routes.size(); i++) {
            packed[i * 2] = routes.get(i).route();
            packed[i * 2 + 1] = routes.get(i).number();
        }
        tag.put("Routes", new IntArrayTag(packed));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = tag.getString("OwnerName");
        name = tag.getString("Name");
        int[] packed = tag.getIntArray("Routes");
        List<RouteStop> read = new ArrayList<>();
        for (int i = 0; i + 1 < packed.length && read.size() < MAX_ROUTES; i += 2) {
            if (packed[i] > 0 && packed[i + 1] > 0) {
                read.add(new RouteStop(packed[i], packed[i + 1]));
            }
        }
        routes = List.copyOf(read);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        write(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
