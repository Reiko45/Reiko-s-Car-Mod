package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: a bus passenger paying the fare ({@link #PAY}, with the fare the screen showed and how), or an
 * owner setting their bus's or taxi's fares ({@link #SET}, as typed).
 */
public record FareActionPayload(int containerId, int action, long expected, Payment payment, String start, String per100)
        implements CustomPacketPayload {
    public static final int PAY = 0;
    public static final int SET = 1;

    public static final Type<FareActionPayload> TYPE = new Type<>(ReikoCars.id("fare_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FareActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FareActionPayload::containerId,
            ByteBufCodecs.VAR_INT, FareActionPayload::action,
            ByteBufCodecs.VAR_LONG, FareActionPayload::expected,
            Payment.STREAM_CODEC, FareActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), FareActionPayload::start,
            ByteBufCodecs.stringUtf8(32), FareActionPayload::per100,
            FareActionPayload::new);

    public static FareActionPayload pay(int containerId, long expected, Payment payment) {
        return new FareActionPayload(containerId, PAY, expected, payment, "", "");
    }

    public static FareActionPayload set(int containerId, String start, String per100) {
        return new FareActionPayload(containerId, SET, 0, Payment.cash(), start, per100);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
