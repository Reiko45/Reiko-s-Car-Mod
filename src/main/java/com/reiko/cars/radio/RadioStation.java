package com.reiko.cars.radio;

import com.reiko.cars.ReikoCars;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The car radio's stations, each playing its own tracks in turn (the sounds {@code reikocars:radio.<track>}). */
public enum RadioStation {
    NEON("neon", "88.1", List.of(new Track("neon_highway", 80.96), new Track("midnight_drive", 77.5))),
    CHILL("chill", "94.5", List.of(new Track("rainy_window", 64.04), new Track("late_bus_home", 65.36))),
    DUSTY("dusty", "101.3", List.of(new Track("gravel_road", 68.71), new Track("dust_on_the_dash", 64.44)));

    /** The quiet between one track and the next, in seconds. */
    public static final double GAP_SECONDS = 2.0;

    /** One of a station's tracks: its name (its sound is {@code reikocars:radio.<name>}) and its length in seconds. */
    public record Track(String name, double seconds) {
    }

    /**
     * Where a station's broadcast is at a moment: which track, how far into it, and how many times the station has
     * been round its tracks (so each play of a track is told apart).
     */
    public record Slot(int track, double offsetSeconds, long round) {
    }

    private final String id;
    private final String frequency;
    private final List<Track> playlist;

    RadioStation(String id, String frequency, List<Track> playlist) {
        this.id = id;
        this.frequency = frequency;
        this.playlist = playlist;
    }

    /** The tracks' names, in the order the station plays them. */
    public List<String> tracks() {
        return playlist.stream().map(Track::name).toList();
    }

    public List<Track> playlist() {
        return playlist;
    }

    /** How long the station takes to play all its tracks once, gaps and all, in seconds. */
    public double roundSeconds() {
        return playlist.stream().mapToDouble(track -> track.seconds() + GAP_SECONDS).sum();
    }

    /**
     * What the station's playing at {@code gameTime}, or null in the quiet between tracks. Every station broadcasts
     * all the time, on the world's clock, so every radio tuned to it plays the same thing at the same moment.
     */
    public Slot at(long gameTime) {
        double round = roundSeconds();
        double t = gameTime / 20.0 + ordinal() * 37.0; // the stations aren't in step with each other
        long rounds = (long) Math.floor(t / round);
        double into = t - rounds * round;
        for (int i = 0; i < playlist.size(); i++) {
            double length = playlist.get(i).seconds();
            if (into < length) {
                return new Slot(i, into, rounds);
            }
            into -= length + GAP_SECONDS;
            if (into < 0) {
                return null;
            }
        }
        return null;
    }

    public String id() {
        return id;
    }

    /** "88.1". */
    public String frequency() {
        return frequency;
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
