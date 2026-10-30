package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from an impound office's screen: pay to get the car picked back ({@link #RELEASE}, at the cost
 * shown, how), let it go free ({@link #RELEASE_FREE}, the lot's owner) or set the fee ({@link #SET_FEE}, as typed);
 * or from a rental desk's: rent the car picked ({@link #RENT}, for {@code value} days, at the cost shown, how) or set
 * a day's rent ({@link #SET_RENT}, for the car type {@code value} by ordinal, as typed); or from a DMV's: pay for the
 * driving test ({@link #TAKE_TEST}) or a new license card ({@link #REPLACE_LICENSE}) at the fee shown, buy the car
 * picked the plate typed ({@link #BUY_PLATE}), or set fee {@code value} ({@link #SET_DMV_FEE}, as typed).
 */
public record LotActionPayload(int containerId, int action, long expected, Payment payment, String text, int value) implements CustomPacketPayload {
    public static final int RELEASE = 0;
    public static final int RELEASE_FREE = 1;
    public static final int SET_FEE = 2;
    public static final int RENT = 3;
    public static final int SET_RENT = 4;
    public static final int TAKE_TEST = 5;
    public static final int REPLACE_LICENSE = 6;
    public static final int BUY_PLATE = 7;
    public static final int SET_DMV_FEE = 8;

    public static final Type<LotActionPayload> TYPE = new Type<>(ReikoCars.id("lot_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LotActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LotActionPayload::containerId,
            ByteBufCodecs.VAR_INT, LotActionPayload::action,
            ByteBufCodecs.VAR_LONG, LotActionPayload::expected,
            Payment.STREAM_CODEC, LotActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), LotActionPayload::text,
            ByteBufCodecs.VAR_INT, LotActionPayload::value,
            LotActionPayload::new);

    public static LotActionPayload pay(int containerId, int action, long expected, Payment payment, int value) {
        return new LotActionPayload(containerId, action, expected, payment, "", value);
    }

    public static LotActionPayload text(int containerId, int action, String text, int value) {
        return new LotActionPayload(containerId, action, 0, Payment.cash(), text, value);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
