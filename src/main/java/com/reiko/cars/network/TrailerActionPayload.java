package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server, from the tanker's screen: load it from the tanks nearby, or unload it into them. */
public record TrailerActionPayload(int entityId, int action) implements CustomPacketPayload {
    public static final int LOAD = 0;
    public static final int UNLOAD = 1;

    public static final Type<TrailerActionPayload> TYPE = new Type<>(ReikoCars.id("trailer_action"));
    public static final StreamCodec<ByteBuf, TrailerActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TrailerActionPayload::entityId,
            ByteBufCodecs.VAR_INT, TrailerActionPayload::action,
            TrailerActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
