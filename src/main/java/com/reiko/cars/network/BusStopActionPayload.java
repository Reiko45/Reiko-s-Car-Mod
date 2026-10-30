package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: a bus stop's owner saving its name and routes (route, stop number pairs; 0, 0 for none). */
public record BusStopActionPayload(int containerId, String name, List<Integer> numbers) implements CustomPacketPayload {
    public static final Type<BusStopActionPayload> TYPE = new Type<>(ReikoCars.id("bus_stop_action"));
    public static final StreamCodec<ByteBuf, BusStopActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BusStopActionPayload::containerId,
            ByteBufCodecs.stringUtf8(64), BusStopActionPayload::name,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(6)), BusStopActionPayload::numbers,
            BusStopActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
