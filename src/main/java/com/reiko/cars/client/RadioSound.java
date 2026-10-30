package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.radio.CarRadio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A car radio's track (or disc), following the car. Riders hear it at the radio's volume; anyone else nearby hears
 * a quarter of that, or half from an open-top car or a bike.
 */
public final class RadioSound extends AbstractTickableSoundInstance {
    private final CarEntity car;

    public RadioSound(CarEntity car, SoundEvent sound, boolean loop) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = loop;
        this.delay = 0;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
        this.volume = level();
    }

    private float level() {
        int state = car.radioState();
        float volume = CarRadio.volume(state) / (float) CarRadio.MAX_VOLUME;
        Minecraft minecraft = Minecraft.getInstance();
        boolean rider = minecraft.player != null && minecraft.player.getVehicle() == car;
        return rider ? volume : volume * (car.carType().openTop() ? 0.5F : 0.25F);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (car.isRemoved()) {
            stop();
            return;
        }
        x = car.getX();
        y = car.getY() + 0.8;
        z = car.getZ();
        volume = level();
    }
}
