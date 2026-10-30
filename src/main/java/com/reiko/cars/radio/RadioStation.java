package com.reiko.cars.radio;

import com.reiko.cars.ReikoCars;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The car radio's stations, each playing its own tracks in turn (the sounds {@code reikocars:radio.<track>}). */
public enum RadioStation {
    NEON("neon", "88.1", List.of("neon_highway", "midnight_drive")),
    CHILL("chill", "94.5", List.of("rainy_window", "late_bus_home")),
    DUSTY("dusty", "101.3", List.of("gravel_road", "dust_on_the_dash"));

    private final String id;
    private final String frequency;
    private final List<String> tracks;

    RadioStation(String id, String frequency, List<String> tracks) {
        this.id = id;
        this.frequency = frequency;
        this.tracks = tracks;
    }

    public String id() {
        return id;
    }

    /** "88.1". */
    public String frequency() {
        return frequency;
    }

    public List<String> tracks() {
        return tracks;
    }

    /** "Neon FM". */
    public Component displayName() {
        return Component.translatable("radio.reikocars.station." + id);
    }

    /** A track's sound event's id. */
    public static ResourceLocation sound(String track) {
        return ReikoCars.id("radio." + track);
    }

    /** A track's title. */
    public static Component title(String track) {
        return Component.translatable("radio.reikocars.track." + track);
    }

    public static RadioStation byIndex(int index) {
        RadioStation[] all = values();
        return all[Math.floorMod(index, all.length)];
    }
}
