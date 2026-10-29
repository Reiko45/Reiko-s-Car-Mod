package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.car.CarType;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

/** A police car's or ambulance's siren, looping and following it while its lights and siren are on. */
public final class SirenSound extends AbstractTickableSoundInstance {
    private final CarEntity car;

    public SirenSound(CarEntity car) {
        super(ModSounds.SIREN.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 2.0F; // carries about 32 blocks
        this.pitch = car.carType() == CarType.AMBULANCE ? 0.9F : 1.0F;
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
        if (car.isRemoved() || !car.sirenOn()) {
            stop();
            return;
        }
        x = car.getX();
        y = car.getY();
        z = car.getZ();
    }
}
