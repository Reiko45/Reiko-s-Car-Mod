package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from a parking meter's screen: pay for {@code minutes} for the car parked there ({@link #PAY}, at
 * the cost shown, how), or the owner setting the rate and fine ({@link #SET}, as typed).
 */
public record MeterActionPayload(int containerId, int action, int minutes, long expected, Payment payment, String rate, String fine)
        implements CustomPacketPayload {
    public static final int PAY = 0;
    public static final int SET = 1;

    public static final Type<MeterActionPayload> TYPE = new Type<>(ReikoCars.id("meter_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MeterActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.containerId());
                buf.writeVarInt(p.action());
                buf.writeVarInt(p.minutes());
                buf.writeVarLong(p.expected());
                Payment.STREAM_CODEC.encode(buf, p.payment());
                buf.writeUtf(p.rate(), 32);
                buf.writeUtf(p.fine(), 32);
            },
            buf -> new MeterActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(),
                    Payment.STREAM_CODEC.decode(buf), buf.readUtf(32), buf.readUtf(32)));

    public static MeterActionPayload pay(int containerId, int minutes, long expected, Payment payment) {
        return new MeterActionPayload(containerId, PAY, minutes, expected, payment, "", "");
    }

    public static MeterActionPayload set(int containerId, String rate, String fine) {
        return new MeterActionPayload(containerId, SET, 0, 0, Payment.cash(), rate, fine);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
