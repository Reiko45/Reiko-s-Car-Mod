package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: buying one of the picked item from an ice cream truck ({@link #BUY}), or its owner pricing it ({@link #PRICE}). */
public record IceCreamActionPayload(int containerId, int action, long expected, Payment payment, String text) implements CustomPacketPayload {
    public static final int BUY = 0;
    public static final int PRICE = 1;

    public static final Type<IceCreamActionPayload> TYPE = new Type<>(ReikoCars.id("ice_cream_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, IceCreamActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, IceCreamActionPayload::containerId,
            ByteBufCodecs.VAR_INT, IceCreamActionPayload::action,
            ByteBufCodecs.VAR_LONG, IceCreamActionPayload::expected,
            Payment.STREAM_CODEC, IceCreamActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), IceCreamActionPayload::text,
            IceCreamActionPayload::new);

    public static IceCreamActionPayload buy(int containerId, long expected, Payment payment) {
        return new IceCreamActionPayload(containerId, BUY, expected, payment, "");
    }

    public static IceCreamActionPayload price(int containerId, String text) {
        return new IceCreamActionPayload(containerId, PRICE, 0, Payment.cash(), text);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
