package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** A car alarm, looping and following the car while it sounds. */
public final class AlarmSound extends AbstractTickableSoundInstance {
    private final CarEntity car;

    public AlarmSound(CarEntity car) {
        super(ModSounds.ALARM.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.6F;
        this.pitch = Math.max(0.85F, Math.min(1.15F, car.carType().hornPitch()));
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !car.isSilent();
    }

    @Override
    public void tick() {
        if (car.isRemoved() || !car.alarmOn()) {
            stop();
            return;
        }
        x = car.getX();
        y = car.getY();
        z = car.getZ();
    }
}
