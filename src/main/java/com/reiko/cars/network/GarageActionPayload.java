package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.garage.GarageSettings;
import com.reiko.economy.shop.Payment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, from a garage's screens: a driver paying to park, or to leave ({@link #PAY}); the owner saving the
 * door's name, switches and prices ({@link #SAVE}), or resetting its remotes ({@link #RESET}).
 */
public record GarageActionPayload(int containerId, int action, int option, long expected, Payment payment, String name, String hour, String day,
                                  int flags) implements CustomPacketPayload {
    public static final int PAY = 0;
    public static final int SAVE = 1;
    public static final int RESET = 2;

    public static final Type<GarageActionPayload> TYPE = new Type<>(ReikoCars.id("garage_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GarageActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.containerId);
                buf.writeVarInt(payload.action);
                buf.writeVarInt(payload.option);
                buf.writeVarLong(payload.expected);
                Payment.STREAM_CODEC.encode(buf, payload.payment);
                buf.writeUtf(payload.name, GarageSettings.MAX_NAME * 2);
                buf.writeUtf(payload.hour, 32);
                buf.writeUtf(payload.day, 32);
                buf.writeVarInt(payload.flags);
            },
            buf -> new GarageActionPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarLong(), Payment.STREAM_CODEC.decode(buf),
                    buf.readUtf(GarageSettings.MAX_NAME * 2), buf.readUtf(32), buf.readUtf(32), buf.readVarInt()));

    public static GarageActionPayload pay(int containerId, int option, long expected, Payment payment) {
        return new GarageActionPayload(containerId, PAY, option, expected, payment, "", "", "", 0);
    }

    public static GarageActionPayload save(int containerId, String name, int flags, String hour, String day) {
        return new GarageActionPayload(containerId, SAVE, 0, 0L, Payment.cash(), name, hour, day, flags);
    }

    public static GarageActionPayload reset(int containerId) {
        return new GarageActionPayload(containerId, RESET, 0, 0L, Payment.cash(), "", "", "", 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
