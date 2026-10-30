package xaero.hud.minimap.waypoint.set;

import xaero.common.minimap.waypoints.Waypoint;

/** Compile-time stand-in for Xaero's Minimap (see build.gradle): only what Reiko's Car Mod uses. */
public abstract class WaypointSet {
    public String getName() {
        throw new UnsupportedOperationException();
    }

    public Iterable<Waypoint> getWaypoints() {
        throw new UnsupportedOperationException();
    }

    public void add(Waypoint waypoint) {
        throw new UnsupportedOperationException();
    }

    public void remove(Waypoint waypoint) {
        throw new UnsupportedOperationException();
    }
}
