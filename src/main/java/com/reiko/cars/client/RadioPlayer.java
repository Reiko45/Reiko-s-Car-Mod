package com.reiko.cars.client;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarEntity;
import com.reiko.cars.radio.CarRadio;
import com.reiko.cars.radio.RadioStation;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import org.jetbrains.annotations.Nullable;

/**
 * Plays every nearby car's radio on this client: a station's tracks one after another, or the disc in its slot over
 * and over, while the radio's on and someone's in the car. Riders see "Now playing" as each track starts.
 */
public final class RadioPlayer {
    /** How close a car must be to be heard. */
    private static final double HEARING = 48.0;

    private static final class Playing {
        @Nullable
        RadioSound sound;
        /** What's playing: which station or disc, which play of which track. A new key starts a new sound. */
        String key = "";
        Component title = Component.empty();
        /** Just the track's name, for the radio's display. */
        Component name = Component.empty();
    }

    private static final Map<CarEntity, Playing> PLAYING = new WeakHashMap<>();
    /** The last "Now Playing" shown to this player, so it shows once per track. */
    private static String shownKey = "";

    private RadioPlayer() {
    }

    /** What {@code car}'s radio is playing here, with the station, or null. */
    @Nullable
    public static Component nowPlaying(CarEntity car) {
        Playing p = PLAYING.get(car);
        return p == null || p.sound == null ? null : p.title;
    }

    /** Just the name of what {@code car}'s radio is playing here, or null. */
    @Nullable
    public static Component trackTitle(CarEntity car) {
        Playing p = PLAYING.get(car);
        return p == null || p.sound == null ? null : p.name;
    }

    private static boolean wanted(CarEntity car) {
        int state = car.radioState();
        if (!CarRadio.on(state) || car.getPassengers().stream().noneMatch(e -> e instanceof net.minecraft.world.entity.player.Player)) {
            return false;
        }
        return CarRadio.source(state) != CarRadio.DISC || !car.radioDiscPlaying().isEmpty();
    }

    /** What a car's radio should be playing now: a key for it, the sound, its titles and where in it to start. */
    private record Broadcast(String key, SoundEvent event, Component title, Component name, double offset) {
    }

    /** What {@code car}'s radio should play at {@code now}, or null between tracks (or with nothing to play). */
    @Nullable
    private static Broadcast broadcast(Minecraft minecraft, CarEntity car, long now) {
        int source = CarRadio.source(car.radioState());
        if (source == CarRadio.DISC) {
            ItemStack disc = car.radioDiscPlaying();
            Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(minecraft.level.registryAccess(), disc);
            if (song.isEmpty()) {
                return null;
            }
            JukeboxSong jukeboxSong = song.get().value();
            long into = CarRadio.discPosition(car.radioStart(), now, jukeboxSong.lengthInTicks());
            if (into < 0) {
                return null; // the pause between plays
            }
            long round = CarRadio.discRound(car.radioStart(), now, jukeboxSong.lengthInTicks());
            Component title = jukeboxSong.description();
            return new Broadcast("disc:" + BuiltInRegistries.ITEM.getKey(disc.getItem()) + ":" + song.get().getRegisteredName() + ":"
                    + car.radioStart() + ":" + round, jukeboxSong.soundEvent().value(), title, title, into / 20.0);
        }
        RadioStation station = RadioStation.byIndex(source);
        RadioStation.Slot slot = station.at(now);
        if (slot == null) {
            return null;
        }
        String track = station.tracks().get(slot.track());
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(RadioStation.sound(track));
        if (event == null) {
            return null;
        }
        Component name = RadioStation.title(track);
        return new Broadcast("station:" + source + ":" + slot.round() + ":" + slot.track(), event,
                Component.translatable("radio.reikocars.track_on", name, station.displayName()), name, slot.offsetSeconds());
    }

    /** Every few ticks: start, change and stop the radios in earshot. */
    static void tick(Minecraft minecraft) {
        SoundManager sounds = minecraft.getSoundManager();
        long now = minecraft.level.getGameTime();
        PLAYING.entrySet().removeIf(entry -> {
            CarEntity car = entry.getKey();
            boolean gone = car.isRemoved() || !wanted(car) || car.distanceToSqr(minecraft.player) > HEARING * HEARING;
            if (gone && entry.getValue().sound != null) {
                sounds.stop(entry.getValue().sound);
            }
            return gone;
        });
        for (CarEntity car : minecraft.level.getEntitiesOfClass(CarEntity.class, minecraft.player.getBoundingBox().inflate(HEARING), RadioPlayer::wanted)) {
            Playing p = PLAYING.computeIfAbsent(car, c -> new Playing());
            Broadcast next = broadcast(minecraft, car, now);
            if (next == null) {
                // between tracks: let the last one finish, unless the radio's been retuned since
                int source = CarRadio.source(car.radioState());
                String prefix = source == CarRadio.DISC ? "disc:" : "station:" + source + ":";
                if (p.sound != null && !p.key.startsWith(prefix)) {
                    sounds.stop(p.sound);
                    p.sound = null;
                    p.key = "";
                }
                continue;
            }
            if (next.key().equals(p.key)) {
                continue; // already playing (or played) this one
            }
            if (p.sound != null) {
                sounds.stop(p.sound);
            }
            p.key = next.key();
            p.title = next.title();
            p.name = next.name();
            p.sound = new RadioSound(car, next.event(), next.offset());
            sounds.play(p.sound);
            ReikoCars.LOGGER.debug("Radio at {} now playing {} from {}s", now, next.title().getString(), next.offset());
        }
        // "Now Playing" for whatever this player's car is playing, once per track
        if (minecraft.player.getVehicle() instanceof CarEntity car) {
            Playing p = PLAYING.get(car);
            if (p != null && p.sound != null && !p.key.equals(shownKey)) {
                minecraft.gui.setNowPlaying(p.title);
                shownKey = p.key;
            }
        }
    }
}
