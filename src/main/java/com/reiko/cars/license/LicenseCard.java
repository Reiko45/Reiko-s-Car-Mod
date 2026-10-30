package com.reiko.cars.license;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a driver's license card says: whose it is, its number, and its points and suspension as they were when it
 * was last updated (the server updates its holder's cards whenever their record changes).
 */
public record LicenseCard(UUID holder, String name, String number, int points, long suspendedUntil) {
    public static final Codec<LicenseCard> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("holder").forGetter(LicenseCard::holder),
            Codec.STRING.fieldOf("name").forGetter(LicenseCard::name),
            Codec.STRING.fieldOf("number").forGetter(LicenseCard::number),
            Codec.INT.optionalFieldOf("points", 0).forGetter(LicenseCard::points),
            Codec.LONG.optionalFieldOf("suspended_until", 0L).forGetter(LicenseCard::suspendedUntil)
    ).apply(instance, LicenseCard::new));

    public static final StreamCodec<ByteBuf, LicenseCard> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, LicenseCard::holder,
            ByteBufCodecs.STRING_UTF8, LicenseCard::name,
            ByteBufCodecs.STRING_UTF8, LicenseCard::number,
            ByteBufCodecs.VAR_INT, LicenseCard::points,
            ByteBufCodecs.VAR_LONG, LicenseCard::suspendedUntil,
            LicenseCard::new);
}
