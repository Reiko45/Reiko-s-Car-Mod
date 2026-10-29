package com.reiko.cars.key;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a cut key fits: the vehicle's lock (a random number the vehicle keeps, changed when its owner changes the
 * locks), and, to show, what kind of vehicle it is (its name's translation key), its colour when the key was cut and
 * its owner's name.
 */
public record CarKey(long code, String vehicle, int color, String owner) {
    public static final Codec<CarKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("code").forGetter(CarKey::code),
            Codec.STRING.fieldOf("vehicle").forGetter(CarKey::vehicle),
            Codec.INT.optionalFieldOf("color", 0x808080).forGetter(CarKey::color),
            Codec.STRING.optionalFieldOf("owner", "").forGetter(CarKey::owner)
    ).apply(instance, CarKey::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CarKey> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, CarKey::code,
            ByteBufCodecs.STRING_UTF8, CarKey::vehicle,
            ByteBufCodecs.INT, CarKey::color,
            ByteBufCodecs.STRING_UTF8, CarKey::owner,
            CarKey::new);

    public CarKey withCode(long newCode) {
        return new CarKey(newCode, vehicle, color, owner);
    }
}
