package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from the pump screen: fill the car ({@link #FILL}) or set the pump's price ({@link #SET_PRICE}).
 *
 * @param millibuckets how much to pump (fill only)
 * @param expectedPrice the price per bucket the screen showed (fill only)
 * @param payment card with PIN, or cash (fill only)
 * @param priceText the new price as typed (set price only)
 */
public record PumpActionPayload(int containerId, int action, int millibuckets, long expectedPrice, Payment payment,
                                String priceText) implements CustomPacketPayload {
    public static final int FILL = 0;
    public static final int SET_PRICE = 1;

    public static final Type<PumpActionPayload> TYPE = new Type<>(ReikoCars.id("pump_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PumpActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PumpActionPayload::containerId,
            ByteBufCodecs.VAR_INT, PumpActionPayload::action,
            ByteBufCodecs.VAR_INT, PumpActionPayload::millibuckets,
            ByteBufCodecs.VAR_LONG, PumpActionPayload::expectedPrice,
            Payment.STREAM_CODEC, PumpActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), PumpActionPayload::priceText,
            PumpActionPayload::new);

    public static PumpActionPayload fill(int containerId, int millibuckets, long expectedPrice, Payment payment) {
        return new PumpActionPayload(containerId, FILL, millibuckets, expectedPrice, payment, "");
    }

    public static PumpActionPayload setPrice(int containerId, String priceText) {
        return new PumpActionPayload(containerId, SET_PRICE, 0, 0, Payment.cash(), priceText);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
