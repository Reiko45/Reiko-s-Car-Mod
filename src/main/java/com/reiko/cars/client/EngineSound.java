package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.registry.ModSounds;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

/**
 * The engine's loop, one per car the client can see. It follows the car, goes quiet while nobody's driving or the
 * tank's empty, and rises in pitch with speed. Starts silent (like a minecart's rolling sound) so it can fade in.
 */
public final class EngineSound extends AbstractTickableSoundInstance {
    private final CarEntity car;
    private float level;

    public EngineSound(CarEntity car) {
        super(ModSounds.ENGINE.get(), SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = car.getX();
        this.y = car.getY();
        this.z = car.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !car.isSilent();
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
        y = car.getY();
        z = car.getZ();
        float target = car.isEngineRunning() ? 1.0F : 0.0F;
        level += (target - level) * 0.15F;
        float pace = (float) Mth.clamp(Math.abs(car.displaySpeed()) / car.carType().topSpeed, 0.0, 1.2);
        volume = level * (0.35F + 0.35F * pace);
        pitch = (0.7F + 0.9F * pace) * car.carType().enginePitch();
    }
}
