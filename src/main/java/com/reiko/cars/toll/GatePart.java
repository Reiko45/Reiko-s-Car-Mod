package com.reiko.cars.toll;

import net.minecraft.util.StringRepresentable;

/** Which block of a toll gate: its pole, or one of the three blocks its arm spans. */
public enum GatePart implements StringRepresentable {
    POLE,
    ARM1,
    ARM2,
    ARM3;

    /** How many blocks along from the pole. */
    public int index() {
        return ordinal();
    }

    public static GatePart at(int index) {
        return values()[index];
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
