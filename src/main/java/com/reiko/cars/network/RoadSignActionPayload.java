package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a traffic light's or speed sign's owner setting its camera's fine (and a sign's limit). */
public record RoadSignActionPayload(int containerId, String fine, String limit, boolean kmh) implements CustomPacketPayload {
    public static final Type<RoadSignActionPayload> TYPE = new Type<>(ReikoCars.id("road_sign_action"));
    public static final StreamCodec<ByteBuf, RoadSignActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RoadSignActionPayload::containerId,
            ByteBufCodecs.stringUtf8(32), RoadSignActionPayload::fine,
            ByteBufCodecs.stringUtf8(8), RoadSignActionPayload::limit,
            ByteBufCodecs.BOOL, RoadSignActionPayload::kmh,
            RoadSignActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
