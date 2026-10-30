package xaero.hud.minimap.waypoint;

/** Compile-time stand-in for Xaero's Minimap (see build.gradle): only what Reiko's Car Mod uses. */
public enum WaypointPurpose {
    NORMAL, DEATH, OLD_DEATH, DESTINATION;

    public boolean isDeath() {
        throw new UnsupportedOperationException();
    }
}
