package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: buying cover at an insurance office for the car picked ({@link #BUY}), or the office's owner
 * setting its premium, deductible and claim limit ({@link #SET}).
 */
public record InsuranceActionPayload(int containerId, int action, long expected, Payment payment, String premium, String deductible,
                                     String limit) implements CustomPacketPayload {
    public static final int BUY = 0;
    public static final int SET = 1;

    public static final Type<InsuranceActionPayload> TYPE = new Type<>(ReikoCars.id("insurance_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InsuranceActionPayload> STREAM_CODEC = StreamCodec.of(
            InsuranceActionPayload::write, InsuranceActionPayload::read);

    private static void write(RegistryFriendlyByteBuf buf, InsuranceActionPayload payload) {
        buf.writeVarInt(payload.containerId);
        buf.writeVarInt(payload.action);
        buf.writeVarLong(payload.expected);
        Payment.STREAM_CODEC.encode(buf, payload.payment);
        buf.writeUtf(payload.premium, 32);
        buf.writeUtf(payload.deductible, 32);
        buf.writeUtf(payload.limit, 32);
    }

    private static InsuranceActionPayload read(RegistryFriendlyByteBuf buf) {
        return new InsuranceActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), Payment.STREAM_CODEC.decode(buf),
                buf.readUtf(32), buf.readUtf(32), buf.readUtf(32));
    }

    public static InsuranceActionPayload buy(int containerId, long expected, Payment payment) {
        return new InsuranceActionPayload(containerId, BUY, expected, payment, "", "", "");
    }

    public static InsuranceActionPayload set(int containerId, String premium, String deductible, String limit) {
        return new InsuranceActionPayload(containerId, SET, 0, Payment.cash(), premium, deductible, limit);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
