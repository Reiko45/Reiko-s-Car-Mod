package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from the driver: what the brakes and crashes have done since the last report. Only the driver's
 * client simulates the car, so only it knows; the server checks the numbers against what the car could have done.
 *
 * @param braked speed the brakes took off, in blocks per tick, summed
 * @param skidded the part of that taken off with the wheels locked by the handbrake
 * @param impact the hardest crash, in blocks per tick (0 for none)
 * @param angle where that crash hit, in radians round from straight ahead, positive to the car's left
 */
public record CarWearPayload(float braked, float skidded, float impact, float angle) implements CustomPacketPayload {
    public static final Type<CarWearPayload> TYPE = new Type<>(ReikoCars.id("car_wear"));
    public static final StreamCodec<ByteBuf, CarWearPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, CarWearPayload::braked,
            ByteBufCodecs.FLOAT, CarWearPayload::skidded,
            ByteBufCodecs.FLOAT, CarWearPayload::impact,
            ByteBufCodecs.FLOAT, CarWearPayload::angle,
            CarWearPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
