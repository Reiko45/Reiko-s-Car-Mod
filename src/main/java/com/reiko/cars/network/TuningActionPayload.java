package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from a Tuning Bench's screen: fit a kit of the player's own ({@link #FIT}, {@code value} the kit),
 * buy the kits picked from the bench's stock ({@link #BUY}, {@code value} a bit per kit, at the total shown, how), set
 * a kit's price ({@link #SET_PRICE}, {@code value} the kit, as typed) or paint the car ({@link #LIVERY}, {@code value}
 * the packed livery).
 */
public record TuningActionPayload(int containerId, int action, int value, long expected, Payment payment, String text) implements CustomPacketPayload {
    public static final int FIT = 0;
    public static final int BUY = 1;
    public static final int SET_PRICE = 2;
    public static final int LIVERY = 3;

    public static final Type<TuningActionPayload> TYPE = new Type<>(ReikoCars.id("tuning_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TuningActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TuningActionPayload::containerId,
            ByteBufCodecs.VAR_INT, TuningActionPayload::action,
            ByteBufCodecs.VAR_INT, TuningActionPayload::value,
            ByteBufCodecs.VAR_LONG, TuningActionPayload::expected,
            Payment.STREAM_CODEC, TuningActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), TuningActionPayload::text,
            TuningActionPayload::new);

    public static TuningActionPayload simple(int containerId, int action, int value) {
        return new TuningActionPayload(containerId, action, value, 0, Payment.cash(), "");
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
