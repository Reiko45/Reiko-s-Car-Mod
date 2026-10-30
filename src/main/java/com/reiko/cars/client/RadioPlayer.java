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
    /** A sound gets this long to start before it's taken as finished. */
    private static final int START_GRACE = 40;

    private static final class Playing {
        @Nullable
        RadioSound sound;
        /** What it's playing: a station's index or CarRadio.DISC. */
        int source = -1;
        ItemStack disc = ItemStack.EMPTY;
        int track = -1;
        long startedAt;
        Component title = Component.empty();
        /** Just the track's name, for the radio's display. */
        Component name = Component.empty();
    }

    private static final Map<CarEntity, Playing> PLAYING = new WeakHashMap<>();

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
            int source = CarRadio.source(car.radioState());
            ItemStack disc = car.radioDiscPlaying();
            boolean retuned = source != p.source || (source == CarRadio.DISC && !ItemStack.isSameItemSameComponents(disc, p.disc));
            if (retuned && p.sound != null) {
                sounds.stop(p.sound);
                p.sound = null;
            }
            boolean finished = p.sound != null && now - p.startedAt > START_GRACE && !sounds.isActive(p.sound);
            if (p.sound != null && !finished) {
                continue;
            }
            // Start the next track: on a new station, start somewhere in its list, different for each car.
            SoundEvent event;
            Component title;
            Component name;
            boolean loop;
            if (source == CarRadio.DISC) {
                Optional<Holder<JukeboxSong>> song = JukeboxSong.fromStack(minecraft.level.registryAccess(), disc);
                if (song.isEmpty()) {
                    continue;
                }
                event = song.get().value().soundEvent().value();
                title = song.get().value().description();
                name = title;
                loop = true;
            } else {
                RadioStation station = RadioStation.byIndex(source);
                int count = station.tracks().size();
                p.track = retuned || p.track < 0 ? Math.floorMod(car.getId(), count) : (p.track + 1) % count;
                String track = station.tracks().get(p.track);
                event = BuiltInRegistries.SOUND_EVENT.get(RadioStation.sound(track));
                if (event == null) {
                    continue;
                }
                name = RadioStation.title(track);
                title = Component.translatable("radio.reikocars.track_on", name, station.displayName());
                loop = false;
            }
            p.source = source;
            p.disc = disc.copy();
            p.title = title;
            p.name = name;
            p.startedAt = now;
            p.sound = new RadioSound(car, event, loop);
            sounds.play(p.sound);
            ReikoCars.LOGGER.debug("Radio in {} now playing {}", car, title.getString());
            if (minecraft.player.getVehicle() == car) {
                minecraft.gui.setNowPlaying(title);
            }
        }
    }
}
