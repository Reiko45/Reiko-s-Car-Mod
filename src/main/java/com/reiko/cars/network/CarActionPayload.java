package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a key pressed while in a car. */
public record CarActionPayload(int action) implements CustomPacketPayload {
    public static final int HORN = 0;
    public static final int TOGGLE_LOCK = 1;
    /** Hitch or unhitch a trailer, or drive on or off a car hauler. */
    public static final int HITCH = 2;
    /** A police car's or ambulance's lights and siren, on or off. */
    public static final int SIREN = 3;
    /** The turn signals, left or right, and the hazards: on, or off if they already were. */
    public static final int SIGNAL_LEFT = 4;
    public static final int SIGNAL_RIGHT = 5;
    public static final int HAZARDS = 6;

    public static final Type<CarActionPayload> TYPE = new Type<>(ReikoCars.id("car_action"));
    public static final StreamCodec<ByteBuf, CarActionPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CarActionPayload::new, CarActionPayload::action);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
