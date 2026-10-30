package com.reiko.cars.client;

import com.reiko.cars.car.CarEntity;
import com.reiko.cars.radio.CarRadio;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * A car radio's track (or disc), following the car, started {@code offset} seconds in so it's in step with the
 * broadcast. Riders hear it at the radio's volume; anyone else nearby hears a quarter of that, or half from an
 * open-top car or a bike.
 */
public final class RadioSound extends AbstractTickableSoundInstance {
    private final CarEntity car;
    private final double offset;

    public RadioSound(CarEntity car, SoundEvent sound, double offset) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.car = car;
        this.offset = Math.max(0.0, offset);
        this.looping = false;
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

    /** The track's stream, wound on to where the broadcast is. (Only streamed sounds can start part way in.) */
    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        CompletableFuture<AudioStream> stream = buffers.getStream(sound.getPath(), false);
        return offset < 0.05 ? stream : stream.thenApply(audio -> new SkippingStream(audio, offset));
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

    /** An audio stream that throws away its first {@code seconds} the first time it's read. */
    private static final class SkippingStream implements AudioStream {
        private final AudioStream inner;
        private long skip;

        SkippingStream(AudioStream inner, double seconds) {
            this.inner = inner;
            AudioFormat format = inner.getFormat();
            this.skip = (long) (seconds * format.getSampleRate()) * format.getFrameSize();
        }

        @Override
        public AudioFormat getFormat() {
            return inner.getFormat();
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            while (skip > 0) {
                ByteBuffer discard = inner.read((int) Math.min(skip, 1 << 16));
                if (discard == null || !discard.hasRemaining()) {
                    skip = 0;
                    return discard;
                }
                skip -= discard.remaining();
                if (skip < 0) {
                    // read past the mark: hand back the rest of this chunk, from the right sample
                    long keep = -skip;
                    skip = 0;
                    ByteBuffer rest = discard.slice((int) (discard.remaining() - keep), (int) keep);
                    ByteBuffer copy = org.lwjgl.BufferUtils.createByteBuffer(rest.remaining());
                    copy.put(rest).flip();
                    return copy;
                }
            }
            return inner.read(size);
        }

        @Override
        public void close() throws IOException {
            inner.close();
        }
    }
}
