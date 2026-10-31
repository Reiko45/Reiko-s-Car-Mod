package com.reiko.cars.dev;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.key.CarKeys;
import com.reiko.cars.registry.ModEntities;
import com.reiko.economy.core.EconomyService;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DEV ONLY (left out of the jar). A load test for a dedicated server: {@code /reikoload start <parked> <driving>} parks
 * that many cars in a grid and puts that many stand-in players at the wheel of others, driving round in circles (moved
 * each tick as a real driver's client would move them); {@code /reikoload stop} takes it all away. Time it with
 * {@code /neoforge tps} and {@code /debug start}/{@code stop}.
 */
@EventBusSubscriber(modid = ReikoCars.MODID)
public final class DevLoadTest {
    /** Where it all happens: the middle of the circles and the parking lot. */
    private static final double CX = 200.5;
    private static final double CZ = 200.5;
    private static final double Y = -60.0;
    private static final CarType[] TYPES = {CarType.SEDAN, CarType.PICKUP_TRUCK, CarType.HATCHBACK, CarType.SPORTS_CAR, CarType.MINIVAN,
            CarType.OFF_ROADER, CarType.CITY_EV, CarType.TAXI};

    private record Driver(ServerPlayer player, CarEntity car, double radius, double speed, double[] angle) {
    }

    private static final List<Driver> DRIVERS = new ArrayList<>();
    /** The stand-ins' connections: what the server sends them is thrown away, and they never time out. */
    private static final List<EmbeddedChannel> CHANNELS = new ArrayList<>();
    private static final List<ServerPlayer> STAND_INS = new ArrayList<>();
    private static final java.lang.reflect.Field KEEP_ALIVE_PENDING;

    static {
        try {
            KEEP_ALIVE_PENDING = net.minecraft.server.network.ServerCommonPacketListenerImpl.class.getDeclaredField("keepAlivePending");
            KEEP_ALIVE_PENDING.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
    private static final List<Entity> PARKED = new ArrayList<>();

    private DevLoadTest() {
    }

    @SubscribeEvent
    static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("reikoload").requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .then(Commands.argument("parked", IntegerArgumentType.integer(0, 2000))
                                .then(Commands.argument("driving", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> {
                                            start(context.getSource().getServer(), IntegerArgumentType.getInteger(context, "parked"),
                                                    IntegerArgumentType.getInteger(context, "driving"));
                                            context.getSource().sendSuccess(() -> Component.literal("REIKOLOAD started"), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("report").executes(context -> {
                    report(context.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("stop").executes(context -> {
                    stop(context.getSource().getServer());
                    context.getSource().sendSuccess(() -> Component.literal("REIKOLOAD stopped"), false);
                    return 1;
                })));
    }

    private static void start(MinecraftServer server, int parked, int driving) {
        stop(server);
        ServerLevel level = server.overworld();
        // The drivers, each at the wheel of a car of their own (with its key), going round a circle of their own.
        for (int i = 0; i < driving; i++) {
            ServerPlayer player = standIn(server, level, "Load_" + i);
            CarType type = TYPES[i % TYPES.length];
            double radius = 12.0 + 4.0 * (i % 8);
            double angle = i * 2.399; // spread round
            CarEntity car = ModEntities.car(type).create(level);
            car.moveTo(CX + Math.cos(angle) * radius, Y, CZ + Math.sin(angle) * radius, 0.0F, 0.0F);
            car.setFuel(type.tankCapacity());
            car.setOwner(player);
            level.addFreshEntity(car);
            player.getInventory().add(CarKeys.keyFor(car));
            player.moveTo(car.getX(), car.getY(), car.getZ());
            player.startRiding(car, true);
            DRIVERS.add(new Driver(player, car, radius, 0.4 + 0.05 * (i % 5), new double[] {angle}));
        }
        // The parking lot: rows of cars, owned by a stand-in (so the GPS keeps track of them), engines off.
        ServerPlayer owner = parked > 0 ? standIn(server, level, "Load_owner") : null;
        int across = Math.max(1, (int) Math.ceil(Math.sqrt(parked)));
        for (int i = 0; i < parked; i++) {
            CarType type = TYPES[i % TYPES.length];
            CarEntity car = ModEntities.car(type).create(level);
            double x = CX - 60.0 + (i % across) * 4.0;
            double z = CZ + 60.0 + (i / across) * 6.0;
            car.moveTo(x, Y, z, 0.0F, 0.0F);
            car.setFuel(type.tankCapacity() * 0.5);
            car.setOwner(owner);
            level.addFreshEntity(car);
            PARKED.add(car);
        }
        if (owner != null) {
            owner.moveTo(CX - 60.0 + across * 2.0, Y + 2.0, CZ + 60.0 + across * 3.0);
        }
        ReikoCars.LOGGER.info("REIKOLOAD started: {} parked, {} driving", parked, driving);
    }

    /** A player logged in with nobody behind it, as the game tests do. */
    private static ServerPlayer standIn(MinecraftServer server, ServerLevel level, String name) {
        UUID id = UUID.nameUUIDFromBytes(("reikoload:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        GameProfile profile = new GameProfile(id, name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(server, level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        CHANNELS.add(new EmbeddedChannel(connection));
        try {
            server.getPlayerList().placeNewPlayer(connection, player, cookie);
        } catch (UnsupportedOperationException e) {
            // BuildCraft's packet as it joins the level, which a connection with nobody behind it can't take: that
            // stops it being added to the level, so add it here (or the chunks round it wouldn't load, nor anything be
            // sent it), as the level would have.
        }
        if (level.getEntity(player.getUUID()) == null) {
            try {
                java.lang.reflect.Field field = ServerLevel.class.getDeclaredField("entityManager");
                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                net.minecraft.world.level.entity.PersistentEntitySectionManager<Entity> entities =
                        (net.minecraft.world.level.entity.PersistentEntitySectionManager<Entity>) field.get(level);
                entities.addNewEntityWithoutEvent(player);
                player.onAddedToLevel();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        EconomyService.INSTANCE.ensureAccount(server, id, name);
        player.setGameMode(GameType.SURVIVAL);
        STAND_INS.add(player);
        return player;
    }

    private static void stop(MinecraftServer server) {
        for (Driver driver : DRIVERS) {
            driver.player.stopRiding();
            driver.car.discard();
            server.getPlayerList().remove(driver.player);
        }
        DRIVERS.clear();
        for (Entity car : PARKED) {
            car.discard();
        }
        PARKED.clear();
        ServerPlayer owner = server.getPlayerList().getPlayerByName("Load_owner");
        if (owner != null) {
            server.getPlayerList().remove(owner);
        }
        STAND_INS.clear();
        for (EmbeddedChannel channel : CHANNELS) {
            channel.releaseOutbound();
        }
        CHANNELS.clear();
    }

    /** Each driver's car on round its circle, as its client would move it. */
    @SubscribeEvent
    static void onTick(ServerTickEvent.Pre event) {
        for (EmbeddedChannel channel : CHANNELS) {
            channel.releaseOutbound();
        }
        for (ServerPlayer player : STAND_INS) {
            try {
                KEEP_ALIVE_PENDING.setBoolean(player.connection, false);
            } catch (ReflectiveOperationException e) {
                ReikoCars.LOGGER.warn("REIKOLOAD can't keep {} connected", player.getGameProfile().getName(), e);
            }
        }
        for (Driver driver : DRIVERS) {
            CarEntity car = driver.car;
            if (!car.isAlive()) {
                continue;
            }
            driver.angle[0] += driver.speed / driver.radius;
            double a = driver.angle[0];
            double x = CX + Math.cos(a) * driver.radius;
            double z = CZ + Math.sin(a) * driver.radius;
            // Heading along the circle (counter-clockwise seen from above: +angle).
            float yaw = (float) Math.toDegrees(a); // facing (-sin yaw, cos yaw): along the circle
            car.setInput(true, false, true, false, false);
            car.setPos(x, car.getY(), z);
            car.setYRot(yaw);
        }
        // Where they are, as their movement packets would tell the server: the chunks round them load and tick.
        for (ServerPlayer player : STAND_INS) {
            if (!player.isRemoved() && player.serverLevel().getEntity(player.getUUID()) == player) {
                player.serverLevel().getChunkSource().move(player);
            }
        }
    }

    /** Logs whether it's all running: cars in ticking chunks, the drivers' fuel going down, the trips counted. */
    private static void report(MinecraftServer server) {
        ServerLevel level = server.overworld();
        int ticking = 0;
        for (Entity car : PARKED) {
            if (level.isPositionEntityTicking(car.blockPosition())) {
                ticking++;
            }
        }
        double fuel = 0.0;
        double trips = 0.0;
        int driving = 0;
        for (Driver driver : DRIVERS) {
            if (level.isPositionEntityTicking(driver.car.blockPosition())) {
                driving++;
            }
            fuel += driver.car.fuel() / driver.car.carType().tankCapacity();
            trips += com.reiko.cars.advancement.CarAdvancements.tripBlocks(driver.player.getUUID());
        }
        ReikoCars.LOGGER.info("REIKOLOAD report: {}/{} parked cars ticking, {}/{} driven cars ticking, fuel left {}%, {} blocks driven in all",
                ticking, PARKED.size(), driving, DRIVERS.size(), DRIVERS.isEmpty() ? 0 : Math.round(fuel / DRIVERS.size() * 100.0), Math.round(trips));
    }
}
