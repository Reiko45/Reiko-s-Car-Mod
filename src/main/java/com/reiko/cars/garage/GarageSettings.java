package com.reiko.cars.garage;

import net.minecraft.nbt.CompoundTag;

/**
 * A garage door's settings, set by its owner: its name, paint, whether its top section has windows, whether it opens by
 * itself for cars, which side is inside, the code its remotes carry, and whether (and for how much) it's run as a
 * parking garage. Every panel keeps a copy, so a door taken apart and put back keeps them.
 *
 * @param color     its paint, or -1 for its style's own
 * @param inward    whether "inside" is the side its panels face, not the side behind them (the door was built from inside)
 * @param code      what a remote linked to it carries; changed when its owner resets the remotes
 * @param hourPrice parking for an hour (a Minecraft hour), in cents
 * @param dayPrice  parking for a day (a Minecraft day), in cents
 */
public record GarageSettings(String name, int color, boolean windows, boolean autoOpen, boolean inward, long code, boolean parking,
                             long hourPrice, long dayPrice) {
    public static final int MAX_NAME = 24;

    public static GarageSettings fresh(long code, long hourPrice, long dayPrice) {
        return new GarageSettings("", -1, true, true, false, code, false, hourPrice, dayPrice);
    }

    public GarageSettings withColor(int newColor) {
        return new GarageSettings(name, newColor, windows, autoOpen, inward, code, parking, hourPrice, dayPrice);
    }

    public GarageSettings withCode(long newCode) {
        return new GarageSettings(name, color, windows, autoOpen, inward, newCode, parking, hourPrice, dayPrice);
    }

    public GarageSettings withInward(boolean newInward) {
        return new GarageSettings(name, color, windows, autoOpen, newInward, code, parking, hourPrice, dayPrice);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);
        tag.putInt("Color", color);
        tag.putBoolean("Windows", windows);
        tag.putBoolean("AutoOpen", autoOpen);
        tag.putBoolean("Inward", inward);
        tag.putLong("Code", code);
        tag.putBoolean("Parking", parking);
        tag.putLong("HourPrice", hourPrice);
        tag.putLong("DayPrice", dayPrice);
        return tag;
    }

    public static GarageSettings load(CompoundTag tag, GarageSettings fallback) {
        if (tag.isEmpty()) {
            return fallback;
        }
        return new GarageSettings(tag.getString("Name"), tag.contains("Color") ? tag.getInt("Color") : -1,
                !tag.contains("Windows") || tag.getBoolean("Windows"), !tag.contains("AutoOpen") || tag.getBoolean("AutoOpen"),
                tag.getBoolean("Inward"), tag.getLong("Code"), tag.getBoolean("Parking"), tag.getLong("HourPrice"), tag.getLong("DayPrice"));
    }
}
