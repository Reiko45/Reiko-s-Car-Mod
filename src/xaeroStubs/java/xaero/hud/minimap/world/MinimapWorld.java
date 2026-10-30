package xaero.hud.minimap.world;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.minimap.waypoint.set.WaypointSet;

/** Compile-time stand-in for Xaero's Minimap (see build.gradle): only what Reiko's Car Mod uses. */
public abstract class MinimapWorld {
    public WaypointSet getCurrentWaypointSet() {
        throw new UnsupportedOperationException();
    }

    public Iterable<WaypointSet> getIterableWaypointSets() {
        throw new UnsupportedOperationException();
    }

    public ResourceKey<Level> getDimId() {
        throw new UnsupportedOperationException();
    }
}
