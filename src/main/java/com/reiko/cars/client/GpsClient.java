package com.reiko.cars.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.gps.GpsInbox;
import com.reiko.cars.gps.GpsMath;
import com.reiko.cars.network.GpsCarsPayload;
import com.reiko.cars.network.GpsRequestPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The GPS, on the client: where it's taking you, if anywhere. The GPS key (N) opens its screen, on foot or driving;
 * pick a place, one of your cars, a waypoint or a spot and the dashboard arrow points the way until you get there.
 * With Xaero's Minimap, the destination and your cars show on the minimap too (see {@link GpsXaero}).
 */
public final class GpsClient {
    public static final KeyMapping KEY = new KeyMapping("key.reikocars.gps", KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM,
            InputConstants.KEY_N, "key.categories.reikocars");
    /** Whether Xaero's Minimap is installed. */
    public static final boolean XAERO = ModList.get().isLoaded("xaerominimap");

    /**
     * Where the GPS is taking you.
     *
     * @param id       a new number for each destination picked (a car moving keeps its destination's)
     * @param pos      where; {@code hasY} false if only the X and Z count
     * @param car      the car, if it's one of yours: it's followed as it moves
     * @param color    for the minimap's waypoint and the screen's lists
     * @param initials for the minimap's waypoint
     */
    public record Destination(int id, Component name, ResourceKey<Level> dimension, Vec3 pos, boolean hasY, @Nullable UUID car, int color,
            String initials) {
        Destination movedTo(Vec3 to, ResourceKey<Level> in) {
            return new Destination(id, name, in, to, hasY, car, color, initials);
        }
    }

    /** One of your cars, where it is now, for the minimap. */
    public record CarPin(UUID id, Vec3 pos, String name, String initials, int color) {
    }

    /** One of Xaero's waypoints, as a place to go. */
    public record WaypointPin(String name, String initials, Vec3 pos, boolean hasY, int color, boolean death) {
    }

    @Nullable
    private static Destination destination;
    private static int nextId = 1;
    /** Your speed over the ground, smoothed, in blocks a tick: for the time to go. */
    private static double speed;
    @Nullable
    private static Vec3 lastPos;
    /** Ticks until the arrival chime's second note; 0 for none. */
    private static int chimeIn;
    private static int ticks;
    /** Set if the Xaero's Minimap integration broke (another version, say): it's left alone after that. */
    private static boolean xaeroBroken;

    private GpsClient() {
    }

    @Nullable
    public static Destination destination() {
        return destination;
    }

    /** Sets off for {@code to}. */
    public static void navigate(Component name, ResourceKey<Level> dimension, Vec3 pos, boolean hasY, @Nullable UUID car, int color,
            String initials) {
        destination = new Destination(nextId++, name, dimension, pos, hasY, car, color, initials);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.5F, 0.5F));
        minecraft.gui.setOverlayMessage(Component.translatable("gps.reikocars.navigating", name), false);
        updateXaero(minecraft);
    }

    /** Stops navigating. */
    public static void stop() {
        destination = null;
        updateXaero(Minecraft.getInstance());
    }

    /** Where the person navigating is: their car, if they're in one. */
    static Entity traveller(LocalPlayer player) {
        return player.getRootVehicle();
    }

    /** Whether the player's driving or riding in a car. */
    static boolean inCar(LocalPlayer player) {
        return player.getRootVehicle() instanceof CarEntity;
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            KEY.consumeClick();
            return;
        }
        while (KEY.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new GpsScreen());
            }
        }
        if (chimeIn > 0 && --chimeIn == 0) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.0F, 0.7F));
        }
        if (minecraft.isPaused()) {
            return;
        }
        ticks++;
        Entity traveller = traveller(player);
        Vec3 at = traveller.position();
        double moved = lastPos == null ? 0.0 : Math.sqrt(Math.pow(at.x - lastPos.x, 2) + Math.pow(at.z - lastPos.z, 2));
        speed = moved > 4.0 ? speed : speed * 0.9 + moved * 0.1; // (not a teleport)
        lastPos = at;
        Destination going = destination;
        if (going != null) {
            if (going.car() != null && ticks % 5 == 0) {
                follow(minecraft, going);
                going = destination;
            }
            if (going != null && going.dimension().equals(minecraft.level.dimension())) {
                double dy = going.hasY() ? going.pos().y - at.y : 0.0;
                if (GpsMath.arrived(going.pos().x - at.x, dy, going.pos().z - at.z, inCar(player))) {
                    arrive(minecraft, going);
                }
            }
        }
        if (XAERO && ticks % 200 == 7 && CarsClientConfig.carsOnMinimap()) {
            PacketDistributor.sendToServer(new GpsRequestPayload(GpsRequestPayload.CARS));
        }
        if (ticks % 10 == 0) {
            updateXaero(minecraft);
        }
    }

    /** Keeps a car destination on the car: where it's loaded, it's seen; otherwise the server's asked now and then. */
    private static void follow(Minecraft minecraft, Destination going) {
        CarEntity car = loadedCar(minecraft, going.car());
        if (car != null) {
            if (car.position().distanceToSqr(going.pos()) > 0.25) {
                destination = going.movedTo(car.position(), minecraft.level.dimension());
            }
            return;
        }
        if (ticks % 100 == 0) {
            PacketDistributor.sendToServer(new GpsRequestPayload(GpsRequestPayload.CARS));
        }
        for (GpsCarsPayload.Entry known : GpsInbox.cars()) {
            if (known.id().equals(going.car())) {
                Vec3 there = known.pos().getBottomCenter();
                if (!known.dimension().equals(going.dimension()) || there.distanceToSqr(going.pos()) > 1.0) {
                    destination = going.movedTo(there, known.dimension());
                }
                return;
            }
        }
    }

    @Nullable
    static CarEntity loadedCar(Minecraft minecraft, @Nullable UUID id) {
        if (id == null || minecraft.level == null) {
            return null;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof CarEntity car && car.getUUID().equals(id)) {
                return car;
            }
        }
        return null;
    }

    private static void arrive(Minecraft minecraft, Destination there) {
        destination = null;
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.33F, 0.7F));
        chimeIn = 5;
        minecraft.gui.setOverlayMessage(Component.translatable("gps.reikocars.arrived", there.name()), false);
        updateXaero(minecraft);
    }

    /** How fast you're going, in blocks a second, for the time to go. */
    static double speedPerSecond() {
        return speed * 20.0;
    }

    /** Your cars, where they are now (the loaded ones as they move), for the minimap; not the one you're in. */
    static List<CarPin> carPins(Minecraft minecraft) {
        List<CarPin> pins = new ArrayList<>();
        if (minecraft.level == null || minecraft.player == null) {
            return pins;
        }
        Entity riding = minecraft.player.getRootVehicle();
        UUID headingFor = destination == null ? null : destination.car();
        for (GpsCarsPayload.Entry known : GpsInbox.cars()) {
            // (not the one you're in, nor the one the GPS is taking you to: that has the GPS's own waypoint)
            if (!known.dimension().equals(minecraft.level.dimension()) || known.id().equals(riding.getUUID()) || known.id().equals(headingFor)) {
                continue;
            }
            CarEntity car = loadedCar(minecraft, known.id());
            Vec3 pos = car != null ? car.position() : known.pos().getBottomCenter();
            pins.add(new CarPin(known.id(), pos, carName(known).getString(), initials(known.type()), known.color()));
        }
        return pins;
    }

    /** "Sedan · ABC 123", or its name tag's name. */
    static Component carName(GpsCarsPayload.Entry car) {
        Component name = car.name().isEmpty() ? Component.translatable("entity.reikocars." + car.type().id()) : Component.literal(car.name());
        return car.plate().isEmpty() ? name : Component.translatable("gps.reikocars.car_name", name, car.plate());
    }

    /** "S" for a sedan, "PT" for a pickup truck: the first letters of its words. */
    static String initials(CarType type) {
        StringBuilder letters = new StringBuilder();
        for (String word : Component.translatable("entity.reikocars." + type.id()).getString().split("[\\s-]+")) {
            if (!word.isEmpty() && letters.length() < 2) {
                letters.appendCodePoint(word.codePointAt(0));
            }
        }
        return letters.isEmpty() ? "C" : letters.toString().toUpperCase(java.util.Locale.ROOT);
    }

    private static void updateXaero(Minecraft minecraft) {
        if (!XAERO || xaeroBroken || minecraft.level == null) {
            return;
        }
        try {
            GpsXaero.update(minecraft, CarsClientConfig.gpsOnMinimap() ? destination : null,
                    CarsClientConfig.carsOnMinimap() ? carPins(minecraft) : null);
        } catch (LinkageError | RuntimeException e) {
            xaeroBroken = true;
            ReikoCars.LOGGER.warn("Couldn't show the GPS on Xaero's Minimap; leaving it alone from now on", e);
        }
    }

    /** Xaero's waypoints in this dimension, as places to go; empty without it. */
    static List<WaypointPin> xaeroWaypoints(Minecraft minecraft) {
        if (!XAERO || xaeroBroken) {
            return List.of();
        }
        try {
            return GpsXaero.waypoints(minecraft);
        } catch (LinkageError | RuntimeException e) {
            xaeroBroken = true;
            ReikoCars.LOGGER.warn("Couldn't read Xaero's Minimap's waypoints; leaving it alone from now on", e);
            return List.of();
        }
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        destination = null;
        lastPos = null;
        speed = 0.0;
        GpsInbox.clear();
        if (XAERO && !xaeroBroken) {
            try {
                GpsXaero.reset();
            } catch (LinkageError | RuntimeException e) {
                xaeroBroken = true;
            }
        }
    }
}
