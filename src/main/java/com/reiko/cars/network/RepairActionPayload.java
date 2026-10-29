package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from the repair bench screen: fit the player's own parts ({@link #FIT}), pay for the bench to
 * fit its parts ({@link #BUY}) or set the price of a part ({@link #SET_PRICE}).
 *
 * @param parts which parts, one bit each by {@code Part} ordinal (setting a price: just the one)
 * @param expectedTotal the bill the screen showed, before tax (buying only)
 * @param payment card with PIN, or cash (buying only)
 * @param priceText the new price as typed (set price only)
 */
public record RepairActionPayload(int containerId, int action, int parts, long expectedTotal, Payment payment,
                                  String priceText) implements CustomPacketPayload {
    public static final int FIT = 0;
    public static final int BUY = 1;
    public static final int SET_PRICE = 2;

    public static final Type<RepairActionPayload> TYPE = new Type<>(ReikoCars.id("repair_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RepairActionPayload::containerId,
            ByteBufCodecs.VAR_INT, RepairActionPayload::action,
            ByteBufCodecs.VAR_INT, RepairActionPayload::parts,
            ByteBufCodecs.VAR_LONG, RepairActionPayload::expectedTotal,
            Payment.STREAM_CODEC, RepairActionPayload::payment,
            ByteBufCodecs.stringUtf8(32), RepairActionPayload::priceText,
            RepairActionPayload::new);

    public static RepairActionPayload fit(int containerId, int parts) {
        return new RepairActionPayload(containerId, FIT, parts, 0, Payment.cash(), "");
    }

    public static RepairActionPayload buy(int containerId, int parts, long expectedTotal, Payment payment) {
        return new RepairActionPayload(containerId, BUY, parts, expectedTotal, payment, "");
    }

    public static RepairActionPayload setPrice(int containerId, int part, String priceText) {
        return new RepairActionPayload(containerId, SET_PRICE, 1 << part, 0, Payment.cash(), priceText);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
