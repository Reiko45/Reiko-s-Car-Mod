package com.reiko.cars.gps;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.FuelType;
import com.reiko.cars.network.GpsCarsPayload;
import com.reiko.cars.network.GpsPlacesPayload;
import com.reiko.cars.network.GpsRequestPayload;
import com.reiko.economy.api.EconomyApi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The GPS's server side: keeps {@link PlacesRegistry} up to date as chunks load and blocks are placed and broken, and
 * answers a GPS's asking for the places nearby (gas stations rather than single pumps, with their prices) and where
 * the asker's cars are.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class GpsService {
    /** Pumps (or benches, or meters) of one owner this close together are one place, in blocks. */
    static final int CLUSTER = 24;
    /** How far round a player the loaded chunks are read again when they open the GPS, in chunks. */
    static final int RESCAN = 8;
    /** A player may ask for places once a second. */
    private static final int COOLDOWN = 20;

    private static final Map<UUID, Long> lastAsked = new HashMap<>();

    private GpsService() {
    }

    // ---- keeping the places up to date ----

    @SubscribeEvent
    static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            PlacesRegistry.get(level.getServer()).scan(chunk);
        }
    }

    @SubscribeEvent
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        rescanSoon(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    static void onBreak(BlockEvent.BreakEvent event) {
        rescanSoon(event.getLevel(), event.getPos());
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        lastAsked.remove(event.getEntity().getUUID());
    }

    /**
     * Once the block at {@code pos} has been placed or broken, reads its chunk and the ones beside it again (a
     * multi-block place's block entity can be a few blocks from the block that was broken).
     */
    private static void rescanSoon(LevelAccessor accessor, BlockPos pos) {
        if (!(accessor instanceof ServerLevel level)) {
            return;
        }
        BlockPos at = pos.immutable();
        level.getServer().execute(() -> {
            for (int dx = -8; dx <= 8; dx += 8) {
                for (int dz = -8; dz <= 8; dz += 8) {
                    rescan(level, new ChunkPos(at.offset(dx, 0, dz)));
                }
            }
        });
    }

    private static void rescan(ServerLevel level, ChunkPos pos) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk != null) {
            PlacesRegistry.get(level.getServer()).scan(chunk);
        }
    }

    // ---- answering a GPS ----

    /** A GPS asks for places or cars. */
    public static void handle(ServerPlayer player, GpsRequestPayload request) {
        if (request.what() == GpsRequestPayload.PLACES) {
            long now = player.serverLevel().getGameTime();
            Long last = lastAsked.get(player.getUUID());
            if (last != null && now - last >= 0 && now - last < COOLDOWN) {
                return;
            }
            lastAsked.put(player.getUUID(), now);
            PacketDistributor.sendToPlayer(player, new GpsPlacesPayload(placesFor(player)));
        } else if (request.what() == GpsRequestPayload.CARS) {
            PacketDistributor.sendToPlayer(player, new GpsCarsPayload(carsFor(player)));
        }
    }

    /** The places in {@code player}'s dimension, nearest first, as their GPS lists them. */
    public static List<GpsPlacesPayload.Entry> placesFor(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        PlacesRegistry registry = PlacesRegistry.get(level.getServer());
        // Catch up with the loaded chunks first: anything built (or knocked down) there by other means than a player.
        ChunkPos here = player.chunkPosition();
        for (int x = -RESCAN; x <= RESCAN; x++) {
            for (int z = -RESCAN; z <= RESCAN; z++) {
                rescan(level, new ChunkPos(here.x + x, here.z + z));
            }
        }
        for (ChunkPos known : registry.chunks(level.dimension())) {
            if (Math.abs(known.x - here.x) > RESCAN || Math.abs(known.z - here.z) > RESCAN) {
                rescan(level, known);
            }
        }
        List<GpsPlacesPayload.Entry> entries = new ArrayList<>();
        for (List<PlacesRegistry.Place> cluster : cluster(registry.in(level.dimension()))) {
            entries.add(entry(cluster));
        }
        BlockPos from = player.blockPosition();
        entries.sort(Comparator.comparingDouble(entry -> entry.pos().distSqr(from)));
        return entries.size() > GpsPlacesPayload.MAX ? List.copyOf(entries.subList(0, GpsPlacesPayload.MAX)) : entries;
    }

    /** {@code player}'s cars, and any they're renting. */
    public static List<GpsCarsPayload.Entry> carsFor(ServerPlayer player) {
        List<GpsCarsPayload.Entry> cars = new ArrayList<>();
        for (CarLocator.Spot spot : CarLocator.get(player.server).carsOf(player.getUUID())) {
            boolean rented = !player.getUUID().equals(spot.owner());
            String name = spot.name().length() > 64 ? spot.name().substring(0, 64) : spot.name();
            String plate = spot.plate().length() > 16 ? spot.plate().substring(0, 16) : spot.plate();
            cars.add(new GpsCarsPayload.Entry(spot.car(), spot.pos().dimension(), spot.pos().pos(), spot.type(), spot.color(), plate, name, rented));
        }
        BlockPos from = player.blockPosition();
        cars.sort(Comparator.comparing((GpsCarsPayload.Entry car) -> car.dimension() != player.level().dimension())
                .thenComparingDouble(car -> car.pos().distSqr(from)));
        return cars.size() > GpsCarsPayload.MAX ? List.copyOf(cars.subList(0, GpsCarsPayload.MAX)) : cars;
    }

    /** Groups places of one kind and owner that are close together: a gas station's pumps, a street's parking meters. */
    static List<List<PlacesRegistry.Place>> cluster(List<PlacesRegistry.Place> places) {
        List<PlacesRegistry.Place> sorted = new ArrayList<>(places);
        sorted.sort(Comparator.comparing(PlacesRegistry.Place::kind).thenComparing(PlacesRegistry.Place::owner)
                .thenComparingLong(place -> place.pos().asLong()));
        List<List<PlacesRegistry.Place>> clusters = new ArrayList<>();
        for (PlacesRegistry.Place place : sorted) {
            List<PlacesRegistry.Place> joined = null;
            for (List<PlacesRegistry.Place> cluster : clusters) {
                PlacesRegistry.Place first = cluster.getFirst();
                if (first.kind() == place.kind() && first.owner().equals(place.owner()) && Math.abs(first.pos().getY() - place.pos().getY()) <= CLUSTER
                        && horizontalDistanceSqr(first.pos(), place.pos()) <= CLUSTER * CLUSTER) {
                    joined = cluster;
                    break;
                }
            }
            if (joined == null) {
                joined = new ArrayList<>();
                clusters.add(joined);
            }
            joined.add(place);
        }
        return clusters;
    }

    private static double horizontalDistanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    /** A cluster as the GPS shows it: in the middle of its members, with their best prices. */
    static GpsPlacesPayload.Entry entry(List<PlacesRegistry.Place> cluster) {
        PlacesRegistry.Place first = cluster.getFirst();
        double x = 0;
        double y = 0;
        double z = 0;
        for (PlacesRegistry.Place place : cluster) {
            x += place.pos().getX();
            y += place.pos().getY();
            z += place.pos().getZ();
        }
        int n = cluster.size();
        BlockPos middle = new BlockPos((int) Math.round(x / n), (int) Math.round(y / n), (int) Math.round(z / n));
        boolean open = cluster.stream().anyMatch(PlacesRegistry.Place::open);
        return new GpsPlacesPayload.Entry(first.kind(), middle, first.owner(), detail(first.kind(), cluster), open);
    }

    /** What to say about a cluster: a gas station's cheapest gasoline and diesel, a car wash's prices, and so on. */
    static Component detail(PlaceKind kind, List<PlacesRegistry.Place> cluster) {
        switch (kind) {
            case GAS -> {
                Map<FuelType, Long> cheapest = new EnumMap<>(FuelType.class);
                for (PlacesRegistry.Place pump : cluster) {
                    if (pump.open() && pump.variant() >= 0 && pump.variant() < FuelType.values().length) {
                        cheapest.merge(FuelType.values()[pump.variant()], pump.price(), Math::min);
                    }
                }
                if (cheapest.isEmpty()) {
                    return Component.translatable("gps.reikocars.detail.no_fuel");
                }
                MutableComponent prices = Component.empty();
                cheapest.forEach((fuel, price) -> {
                    if (!prices.getSiblings().isEmpty()) {
                        prices.append(" · ");
                    }
                    prices.append(Component.translatable("gps.reikocars.detail.fuel_price", fuel.displayName(), price(price)));
                });
                return Component.translatable("gps.reikocars.detail.per_bucket", prices);
            }
            case CHARGER -> {
                long cheapest = cluster.stream().mapToLong(PlacesRegistry.Place::price).min().orElse(0);
                return Component.translatable("gps.reikocars.detail.charge_price", price(cheapest));
            }
            case CAR_WASH -> {
                long tunnel = Long.MAX_VALUE;
                long bay = Long.MAX_VALUE;
                for (PlacesRegistry.Place wash : cluster) {
                    if (wash.open()) {
                        if (wash.variant() == 1) {
                            bay = Math.min(bay, wash.price());
                        } else {
                            tunnel = Math.min(tunnel, wash.price());
                        }
                    }
                }
                if (tunnel == Long.MAX_VALUE && bay == Long.MAX_VALUE) {
                    return Component.translatable("gps.reikocars.detail.closed");
                }
                MutableComponent prices = Component.empty();
                if (tunnel != Long.MAX_VALUE) {
                    prices.append(Component.translatable("gps.reikocars.detail.tunnel", price(tunnel)));
                }
                if (bay != Long.MAX_VALUE) {
                    if (!prices.getSiblings().isEmpty()) {
                        prices.append(" · ");
                    }
                    prices.append(Component.translatable("gps.reikocars.detail.bay", price(bay)));
                }
                return prices;
            }
            case PARKING -> {
                return cluster.size() > 1 ? Component.translatable("gps.reikocars.detail.meters", cluster.size()) : Component.empty();
            }
            default -> {
                return Component.empty();
            }
        }
    }

    private static Component price(long cents) {
        return cents <= 0 ? Component.translatable("gps.reikocars.free") : Component.literal(EconomyApi.get().format(cents));
    }
}
