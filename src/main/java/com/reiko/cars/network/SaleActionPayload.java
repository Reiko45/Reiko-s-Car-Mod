package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from a car's sale screen or a dealership's: buy the car ({@link #BUY}, at the price shown, how),
 * put it up for sale ({@link #SET_PRICE}, as typed) or take it off ({@link #OFF_SALE}); at a dealership, open the
 * buying screen for the car picked ({@link #OPEN_BUY}), trade the car picked in ({@link #TRADE_IN}, at the offer shown),
 * or set a trade-in offer ({@link #SET_OFFER}, for the car type by ordinal, as typed).
 */
public record SaleActionPayload(int containerId, int action, long expected, Payment payment, String text, int carType)
        implements CustomPacketPayload {
    public static final int BUY = 0;
    public static final int SET_PRICE = 1;
    public static final int OFF_SALE = 2;
    public static final int OPEN_BUY = 3;
    public static final int TRADE_IN = 4;
    public static final int SET_OFFER = 5;
    /** Buy the car on finance: the down payment now, by card or cash, the rest a day at a time. */
    public static final int FINANCE = 6;
    /** At a dealership: pay off the picked car's loan, from the bank. */
    public static final int PAY_OFF = 7;

    public static final Type<SaleActionPayload> TYPE = new Type<>(ReikoCars.id("sale_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SaleActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SaleActionPayload::containerId,
            ByteBufCodecs.VAR_INT, SaleActionPayload::action,
            ByteBufCodecs.VAR_LONG, SaleActionPayload::expected,
            Payment.STREAM_CODEC, SaleActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), SaleActionPayload::text,
            ByteBufCodecs.VAR_INT, SaleActionPayload::carType,
            SaleActionPayload::new);

    public static SaleActionPayload of(int containerId, int action) {
        return new SaleActionPayload(containerId, action, 0, Payment.cash(), "", 0);
    }

    public static SaleActionPayload buy(int containerId, long expected, Payment payment) {
        return new SaleActionPayload(containerId, BUY, expected, payment, "", 0);
    }

    public static SaleActionPayload finance(int containerId, long expected, Payment payment) {
        return new SaleActionPayload(containerId, FINANCE, expected, payment, "", 0);
    }

    public static SaleActionPayload text(int containerId, int action, String text, int carType) {
        return new SaleActionPayload(containerId, action, 0, Payment.cash(), text, carType);
    }

    public static SaleActionPayload tradeIn(int containerId, long expected) {
        return new SaleActionPayload(containerId, TRADE_IN, expected, Payment.cash(), "", 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
