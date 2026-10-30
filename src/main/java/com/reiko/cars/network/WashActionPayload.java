package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: paying at a Car Wash or Wash Bay ({@link #PAY}), or its owner setting the price ({@link #SET}). */
public record WashActionPayload(int containerId, int action, long expected, Payment payment, String text) implements CustomPacketPayload {
    public static final int PAY = 0;
    public static final int SET = 1;

    public static final Type<WashActionPayload> TYPE = new Type<>(ReikoCars.id("wash_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WashActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WashActionPayload::containerId,
            ByteBufCodecs.VAR_INT, WashActionPayload::action,
            ByteBufCodecs.VAR_LONG, WashActionPayload::expected,
            Payment.STREAM_CODEC, WashActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), WashActionPayload::text,
            WashActionPayload::new);

    public static WashActionPayload pay(int containerId, long expected, Payment payment) {
        return new WashActionPayload(containerId, PAY, expected, payment, "");
    }

    public static WashActionPayload set(int containerId, String text) {
        return new WashActionPayload(containerId, SET, 0, Payment.cash(), text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
