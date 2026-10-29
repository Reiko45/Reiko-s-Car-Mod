package com.reiko.cars.insurance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A car's insurance: who insures it (the office's owner, who pays the claims) and for whom (the car's owner when the
 * cover was bought; it lapses if the car changes hands), until when (game time), and the terms it was sold on.
 * Kept on the car, and on its item when it's picked up.
 *
 * @param until      the game time cover runs out
 * @param deductible what the car's owner pays of each claim, in cents
 * @param limit      the most one claim pays out, in cents
 * @param office     the insurance office it was bought at, for its books
 */
public record Policy(UUID insurer, String insurerName, UUID holder, long until, long deductible, long limit, Optional<GlobalPos> office) {
    public static final Codec<Policy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("insurer").forGetter(Policy::insurer),
            Codec.STRING.fieldOf("insurer_name").forGetter(Policy::insurerName),
            UUIDUtil.CODEC.fieldOf("holder").forGetter(Policy::holder),
            Codec.LONG.fieldOf("until").forGetter(Policy::until),
            Codec.LONG.fieldOf("deductible").forGetter(Policy::deductible),
            Codec.LONG.fieldOf("limit").forGetter(Policy::limit),
            GlobalPos.CODEC.optionalFieldOf("office").forGetter(Policy::office)
    ).apply(instance, Policy::new));

    public static final StreamCodec<ByteBuf, Policy> STREAM_CODEC = StreamCodec.of(Policy::write, Policy::read);

    private static void write(ByteBuf buf, Policy policy) {
        UUIDUtil.STREAM_CODEC.encode(buf, policy.insurer);
        ByteBufCodecs.STRING_UTF8.encode(buf, policy.insurerName);
        UUIDUtil.STREAM_CODEC.encode(buf, policy.holder);
        ByteBufCodecs.VAR_LONG.encode(buf, policy.until);
        ByteBufCodecs.VAR_LONG.encode(buf, policy.deductible);
        ByteBufCodecs.VAR_LONG.encode(buf, policy.limit);
        ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).encode(buf, policy.office);
    }

    private static Policy read(ByteBuf buf) {
        return new Policy(UUIDUtil.STREAM_CODEC.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf), UUIDUtil.STREAM_CODEC.decode(buf),
                ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_LONG.decode(buf),
                ByteBufCodecs.optional(GlobalPos.STREAM_CODEC).decode(buf));
    }

    /** Whether it covers a car owned by {@code owner} at game time {@code now}. */
    public boolean covers(UUID owner, long now) {
        return holder.equals(owner) && now < until;
    }

    /** Whole Minecraft days of cover left at {@code now}, rounded up: 0 once it's run out. */
    public int daysLeft(long now) {
        return now >= until ? 0 : (int) ((until - now + 23_999) / 24_000);
    }

    /** What a claim for {@code cost} of repairs pays out: the cost above the deductible, up to the limit. */
    public long payout(long cost) {
        return Math.max(0, Math.min(limit, cost - deductible));
    }
}
