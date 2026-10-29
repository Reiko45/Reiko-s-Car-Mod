package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: paying the toll at a gate ({@link #PAY}), or a booth's owner setting the toll ({@link #SET}). */
public record TollActionPayload(int containerId, int action, long expected, Payment payment, String text) implements CustomPacketPayload {
    public static final int PAY = 0;
    public static final int SET = 1;

    public static final Type<TollActionPayload> TYPE = new Type<>(ReikoCars.id("toll_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TollActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TollActionPayload::containerId,
            ByteBufCodecs.VAR_INT, TollActionPayload::action,
            ByteBufCodecs.VAR_LONG, TollActionPayload::expected,
            Payment.STREAM_CODEC, TollActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), TollActionPayload::text,
            TollActionPayload::new);

    public static TollActionPayload pay(int containerId, long expected, Payment payment) {
        return new TollActionPayload(containerId, PAY, expected, payment, "");
    }

    public static TollActionPayload set(int containerId, String text) {
        return new TollActionPayload(containerId, SET, 0, Payment.cash(), text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
