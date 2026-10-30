package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the GPS asking for the places near you ({@link #PLACES}) or where your cars are ({@link #CARS}). */
public record GpsRequestPayload(int what) implements CustomPacketPayload {
    public static final int PLACES = 0;
    public static final int CARS = 1;

    public static final Type<GpsRequestPayload> TYPE = new Type<>(ReikoCars.id("gps_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GpsRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GpsRequestPayload::what,
            GpsRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
