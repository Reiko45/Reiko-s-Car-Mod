package com.reiko.cars.client;

import com.reiko.cars.ReikoCars;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointVisibilityType;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;

/**
 * The GPS on Xaero's Minimap. Only loaded when that mod is installed ({@link GpsClient#XAERO}).
 * <ul>
 *   <li>The destination is a temporary waypoint in the current waypoint set, so it's on the minimap, in the world and on
 *   Xaero's World Map; it goes when you get there or stop navigating, and isn't saved. (If you delete it in Xaero's
 *   menu, it's left off until you pick somewhere new.)</li>
 *   <li>Your cars are the mod's own waypoints: on the minimap and in the world, not in your waypoint list.</li>
 *   <li>The waypoints in this dimension (and your death point) are places the GPS can take you.</li>
 * </ul>
 */
final class GpsXaero {
    private static final ResourceLocation CARS = ReikoCars.id("cars");

    /** The destination's waypoint, the set it's in and the destination it's for. */
    @Nullable
    private static Waypoint shown;
    @Nullable
    private static WaypointSet shownIn;
    private static int shownFor;
    /** The destination whose waypoint was deleted in Xaero's menu: not put back. */
    private static int dismissed;

    private GpsXaero() {
    }

    @Nullable
    private static MinimapWorldManager manager() {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        return session == null ? null : session.getWorldManager();
    }

    /** The waypoint world for the dimension the player's in, or null if Xaero's showing another. */
    @Nullable
    private static MinimapWorld here(Minecraft minecraft, @Nullable MinimapWorldManager manager) {
        MinimapWorld world = manager == null ? null : manager.getCurrentWorld();
        return world != null && minecraft.level != null && minecraft.level.dimension().equals(world.getDimId()) ? world : null;
    }

    /** Puts the destination (or nothing, if null) and the cars (or none, if null) on the minimap. */
    static void update(Minecraft minecraft, @Nullable GpsClient.Destination destination, @Nullable List<GpsClient.CarPin> cars) {
        MinimapWorldManager manager = manager();
        MinimapWorld world = here(minecraft, manager);
        showDestination(world, destination != null && destination.dimension().equals(minecraft.level.dimension()) ? destination : null);
        if (manager != null) {
            showCars(manager.getCustomWaypoints(CARS), world == null ? null : cars);
        }
    }

    private static void showDestination(@Nullable MinimapWorld world, @Nullable GpsClient.Destination destination) {
        WaypointSet set = world == null || destination == null || destination.id() == dismissed ? null : world.getCurrentWaypointSet();
        if (shown != null && (set == null || set != shownIn || destination.id() != shownFor)) {
            removeShown();
        }
        if (set == null) {
            return;
        }
        int x = (int) Math.floor(destination.pos().x);
        int y = (int) Math.floor(destination.pos().y);
        int z = (int) Math.floor(destination.pos().z);
        if (shown == null) {
            Waypoint waypoint = new Waypoint(x, y, z, destination.name().getString(), "GPS", nearest(destination.color()), WaypointPurpose.NORMAL,
                    true, destination.hasY());
            waypoint.setVisibility(WaypointVisibilityType.GLOBAL); // always shown, however far
            set.add(waypoint);
            shown = waypoint;
            shownIn = set;
            shownFor = destination.id();
            refreshWorldMap();
        } else if (!contains(set, shown)) {
            // Deleted in Xaero's menu: left off till the next destination.
            dismissed = destination.id();
            shown = null;
            shownIn = null;
        } else if (shown.getX() != x || shown.getY() != y || shown.getZ() != z) {
            shown.setX(x);
            shown.setY(y);
            shown.setZ(z);
            refreshWorldMap();
        }
    }

    private static void removeShown() {
        if (shown != null && shownIn != null) {
            shownIn.remove(shown);
            refreshWorldMap();
        }
        shown = null;
        shownIn = null;
    }

    private static boolean contains(WaypointSet set, Waypoint waypoint) {
        for (Waypoint in : set.getWaypoints()) {
            if (in == waypoint) {
                return true;
            }
        }
        return false;
    }

    private static void showCars(Int2ObjectMap<Waypoint> pins, @Nullable List<GpsClient.CarPin> cars) {
        if (cars == null || cars.isEmpty()) {
            pins.clear();
            return;
        }
        IntSet keep = new IntOpenHashSet();
        for (GpsClient.CarPin car : cars) {
            int key = car.id().hashCode();
            keep.add(key);
            int x = (int) Math.floor(car.pos().x);
            int y = (int) Math.floor(car.pos().y);
            int z = (int) Math.floor(car.pos().z);
            Waypoint pin = pins.get(key);
            if (pin == null) {
                pin = new Waypoint(x, y, z, car.name(), car.initials(), nearest(car.color()), WaypointPurpose.NORMAL, true, true);
                pins.put(key, pin);
            } else {
                pin.setX(x);
                pin.setY(y);
                pin.setZ(z);
                if (!pin.getName().equals(car.name())) {
                    pin.setName(car.name());
                }
                pin.setWaypointColor(nearest(car.color()));
            }
        }
        pins.keySet().removeIf((java.util.function.IntPredicate) key -> !keep.contains(key));
    }

    /** Xaero's waypoints in the dimension the player's in, as places to go (not the GPS's own). */
    static List<GpsClient.WaypointPin> waypoints(Minecraft minecraft) {
        List<GpsClient.WaypointPin> pins = new ArrayList<>();
        MinimapWorld world = here(minecraft, manager());
        if (world == null) {
            return pins;
        }
        for (WaypointSet set : world.getIterableWaypointSets()) {
            for (Waypoint waypoint : set.getWaypoints()) {
                if (waypoint == shown) {
                    continue;
                }
                Vec3 pos = new Vec3(waypoint.getX() + 0.5, waypoint.getY(), waypoint.getZ() + 0.5);
                String initials = waypoint.getInitials();
                pins.add(new GpsClient.WaypointPin(waypoint.getLocalizedName(), initials == null ? "" : initials, pos, waypoint.isYIncluded(),
                        0xFF000000 | waypoint.getWaypointColor().getHex(), waypoint.getPurpose().isDeath()));
            }
        }
        return pins;
    }

    /** Takes it all off the minimap, on leaving a world. */
    static void reset() {
        removeShown();
        dismissed = 0;
        MinimapWorldManager manager = manager();
        if (manager != null) {
            manager.getCustomWaypoints(CARS).clear();
        }
    }

    /** Xaero's color nearest {@code rgb}. */
    static WaypointColor nearest(int rgb) {
        WaypointColor best = WaypointColor.WHITE;
        long bestDistance = Long.MAX_VALUE;
        for (WaypointColor color : WaypointColor.values()) {
            int hex = color.getHex();
            long dr = ((hex >> 16) & 0xFF) - ((rgb >> 16) & 0xFF);
            long dg = ((hex >> 8) & 0xFF) - ((rgb >> 8) & 0xFF);
            long db = (hex & 0xFF) - (rgb & 0xFF);
            long distance = dr * dr * 3 + dg * dg * 4 + db * db * 2;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = color;
            }
        }
        return best;
    }

    private static boolean worldMapBroken;

    /** Xaero's World Map only reads the waypoints again when asked. */
    private static void refreshWorldMap() {
        if (worldMapBroken || !ModList.get().isLoaded("xaeroworldmap")) {
            return;
        }
        try {
            Object minimap = Class.forName("xaero.map.mods.SupportMods").getField("xaeroMinimap").get(null);
            if (minimap != null) {
                minimap.getClass().getMethod("requestWaypointsRefresh").invoke(minimap);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            worldMapBroken = true;
        }
    }
}
