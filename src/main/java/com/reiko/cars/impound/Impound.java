package com.reiko.cars.impound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

/**
 * A car held at an impound lot: which lot (its office), whose lot it is, since when (game time), and why: towed in from
 * an expired parking meter ({@link #PARKING}), or repossessed for a car loan's missed payments ({@link #REPOSSESSED}),
 * when its owner must also pay those ({@code arrears}, in cents) to get it back. Kept on the car while it's held.
 */
public record Impound(BlockPos lot, UUID lotOwner, String lotOwnerName, long since, String reason, long arrears, Optional<UUID> loan) {
    public static final String PARKING = "parking";
    public static final String REPOSSESSED = "repossessed";

    public static final Codec<Impound> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("lot").forGetter(Impound::lot),
            UUIDUtil.CODEC.fieldOf("lot_owner").forGetter(Impound::lotOwner),
            Codec.STRING.fieldOf("lot_owner_name").forGetter(Impound::lotOwnerName),
            Codec.LONG.fieldOf("since").forGetter(Impound::since),
            Codec.STRING.fieldOf("reason").forGetter(Impound::reason),
            Codec.LONG.optionalFieldOf("arrears", 0L).forGetter(Impound::arrears),
            UUIDUtil.CODEC.optionalFieldOf("loan").forGetter(Impound::loan)
    ).apply(instance, Impound::new));

    public boolean repossessed() {
        return REPOSSESSED.equals(reason);
    }

    /** Whole Minecraft days it's been held at {@code now}, rounded down. */
    public int daysHeld(long now) {
        return (int) Math.max(0, (now - since) / 24_000);
    }
}
