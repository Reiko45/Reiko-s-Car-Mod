package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from the mailbox screen: send what's packed to the player named {@code recipient}.
 *
 * @param expectedPostage the postage the screen showed, before tax (0 when dropping it in the addressee's own mailbox)
 * @param payment card with PIN, or cash
 */
public record MailActionPayload(int containerId, String recipient, long expectedPostage, Payment payment) implements CustomPacketPayload {
    public static final Type<MailActionPayload> TYPE = new Type<>(ReikoCars.id("mail_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MailActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MailActionPayload::containerId,
            ByteBufCodecs.stringUtf8(32), MailActionPayload::recipient,
            ByteBufCodecs.VAR_LONG, MailActionPayload::expectedPostage,
            Payment.STREAM_CODEC, MailActionPayload::payment,
            MailActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
