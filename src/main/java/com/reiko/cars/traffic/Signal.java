package com.reiko.cars.traffic;

import net.minecraft.util.StringRepresentable;

/** A traffic light's lamp. */
public enum Signal implements StringRepresentable {
    GREEN("green"),
    YELLOW("yellow"),
    RED("red");

    private final String name;

    Signal(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
