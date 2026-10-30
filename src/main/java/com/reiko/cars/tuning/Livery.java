package com.reiko.cars.tuning;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;

/** A car's race number (1 to 99 on roundels on its doors, 0 for none) and racing stripes (a dye colour, or none). */
public record Livery(int number, int stripes) {
    public static final int MAX_NUMBER = 99;
    public static final Livery PLAIN = new Livery(0, 0);

    public static final Codec<Livery> CODEC = Codec.INT.xmap(Livery::unpack, Livery::pack);
    public static final StreamCodec<ByteBuf, Livery> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Livery::unpack, Livery::pack);

    public Livery {
        number = Math.max(0, Math.min(MAX_NUMBER, number));
        stripes = Math.max(0, Math.min(DyeColor.values().length, stripes));
    }

    public int pack() {
        return number | stripes << 7;
    }

    public static Livery unpack(int packed) {
        return new Livery(packed & 127, packed >> 7 & 31);
    }

    public boolean isPlain() {
        return number == 0 && stripes == 0;
    }

    /** The stripes' colour, or null for none. */
    @Nullable
    public DyeColor stripeColor() {
        return stripes == 0 ? null : DyeColor.byId(stripes - 1);
    }

    public static Livery of(int number, @Nullable DyeColor stripes) {
        return new Livery(number, stripes == null ? 0 : stripes.getId() + 1);
    }
}
