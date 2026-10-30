package com.reiko.cars.radio;

import com.reiko.cars.car.CarEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * A car radio's settings, packed into one int synced with the car: whether it's on, what it plays (one of the
 * {@link RadioStation}s, or the disc in its slot) and its volume, 0 to 10. It plays while someone's in the car; its
 * riders hear it at full volume, and anyone nearby faintly (less faintly from an open-top car or a bike).
 */
public final class CarRadio {
    /** What the radio plays when it's set to the disc slot. */
    public static final int DISC = RadioStation.values().length;
    public static final int MAX_VOLUME = 10;
    /** A new car's radio: off, on the first station, at volume 6. */
    public static final int DEFAULT = pack(false, 0, 6);

    // The radio screen's buttons
    public static final int BUTTON_POWER = 0;
    /** Buttons 1 to 3 pick a station; 4 the disc. */
    public static final int BUTTON_SOURCE = 1;
    public static final int BUTTON_QUIETER = 5;
    public static final int BUTTON_LOUDER = 6;

    private CarRadio() {
    }

    public static int pack(boolean on, int source, int volume) {
        return (on ? 0x100 : 0) | ((source & 0xF) << 4) | (Math.max(0, Math.min(MAX_VOLUME, volume)) & 0xF);
    }

    public static boolean on(int state) {
        return (state & 0x100) != 0;
    }

    /** A station's index, or {@link #DISC}. */
    public static int source(int state) {
        int source = (state >> 4) & 0xF;
        return source > DISC ? 0 : source;
    }

    public static int volume(int state) {
        return Math.min(MAX_VOLUME, state & 0xF);
    }

    /** One of the radio screen's buttons. Returns whether it did anything. */
    public static boolean press(CarEntity car, int button) {
        int state = car.radioState();
        boolean on = on(state);
        int source = source(state);
        int volume = volume(state);
        if (button == BUTTON_POWER) {
            on = !on;
        } else if (button >= BUTTON_SOURCE && button <= BUTTON_SOURCE + DISC) {
            source = button - BUTTON_SOURCE;
            on = true; // picking a station turns it on
        } else if (button == BUTTON_QUIETER) {
            volume = Math.max(0, volume - 1);
        } else if (button == BUTTON_LOUDER) {
            volume = Math.min(MAX_VOLUME, volume + 1);
        } else {
            return false;
        }
        car.setRadioState(pack(on, source, volume));
        return true;
    }

    /** Whether {@code stack} can go in a car radio's disc slot: a music disc, or anything else a jukebox plays. */
    public static boolean playable(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE);
    }
}
