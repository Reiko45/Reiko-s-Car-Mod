package xaero.hud.module;

/** Compile-time stand-in for Xaero's Minimap (see build.gradle): only what Reiko's Car Mod uses. */
public final class HudModule<MS extends ModuleSession<MS>> {
    public MS getCurrentSession() {
        throw new UnsupportedOperationException();
    }
}
