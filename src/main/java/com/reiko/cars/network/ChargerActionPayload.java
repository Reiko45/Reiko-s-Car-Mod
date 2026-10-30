package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from the charging screen: charge the car ({@link #CHARGE}) or set the station's price
 * ({@link #SET_PRICE}).
 *
 * @param mj how much to charge into the car
 * @param expectedPrice the price per 1,000 MJ the screen showed (charging only)
 * @param payment card with PIN, or cash (charging only)
 * @param priceText the new price as typed (set price only)
 */
public record ChargerActionPayload(int containerId, int action, long mj, long expectedPrice, Payment payment, String priceText)
        implements CustomPacketPayload {
    public static final int CHARGE = 0;
    public static final int SET_PRICE = 1;

    public static final Type<ChargerActionPayload> TYPE = new Type<>(ReikoCars.id("charger_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ChargerActionPayload::containerId,
            ByteBufCodecs.VAR_INT, ChargerActionPayload::action,
            ByteBufCodecs.VAR_LONG, ChargerActionPayload::mj,
            ByteBufCodecs.VAR_LONG, ChargerActionPayload::expectedPrice,
            Payment.STREAM_CODEC, ChargerActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), ChargerActionPayload::priceText,
            ChargerActionPayload::new);

    public static ChargerActionPayload charge(int containerId, long mj, long expectedPrice, Payment payment) {
        return new ChargerActionPayload(containerId, CHARGE, mj, expectedPrice, payment, "");
    }

    public static ChargerActionPayload setPrice(int containerId, String priceText) {
        return new ChargerActionPayload(containerId, SET_PRICE, 0, 0, Payment.cash(), priceText);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
