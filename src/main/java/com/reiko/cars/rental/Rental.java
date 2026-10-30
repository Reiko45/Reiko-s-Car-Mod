package com.reiko.cars.rental;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.phys.Vec3;

/**
 * A car out on rent: who to, from which rental desk, where it was parked on the lot (it goes back there when the
 * rental's over), until when (game time), and whether it has left the lot yet (bringing it back after it has ends the
 * rental early). Kept on the car while it's rented.
 */
public record Rental(UUID renter, String renterName, BlockPos desk, Vec3 home, float homeYaw, long until, boolean leftLot) {
    public static final Codec<Rental> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("renter").forGetter(Rental::renter),
            Codec.STRING.fieldOf("renter_name").forGetter(Rental::renterName),
            BlockPos.CODEC.fieldOf("desk").forGetter(Rental::desk),
            Vec3.CODEC.fieldOf("home").forGetter(Rental::home),
            Codec.FLOAT.fieldOf("home_yaw").forGetter(Rental::homeYaw),
            Codec.LONG.fieldOf("until").forGetter(Rental::until),
            Codec.BOOL.optionalFieldOf("left_lot", false).forGetter(Rental::leftLot)
    ).apply(instance, Rental::new));

    public boolean active(long now) {
        return now < until;
    }

    public Rental leaving() {
        return new Rental(renter, renterName, desk, home, homeYaw, until, true);
    }
}
