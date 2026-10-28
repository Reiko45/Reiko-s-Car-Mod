package com.reiko.cars.workbench;

import java.util.Locale;

/**
 * The parts made for one kind of car, which go together at a Car Workbench with a car engine and four tires. Each car
 * has its own chassis, body and interior: {@code sedan_chassis}, {@code sports_car_body} and so on.
 */
public enum CarPart {
    CHASSIS,
    BODY,
    INTERIOR;

    private final String id = name().toLowerCase(Locale.ROOT);

    public String id() {
        return id;
    }
}
