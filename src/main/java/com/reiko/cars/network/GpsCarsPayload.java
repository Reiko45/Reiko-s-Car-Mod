package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.car.CarType;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/** Server to client: where your cars (and any you're renting) were last seen, for the GPS. */
public record GpsCarsPayload(List<Entry> cars) implements CustomPacketPayload {
    public static final int MAX = 64;

    /**
     * A car.
     *
     * @param name   its name tag's name, or "" for none
     * @param rented whether you're renting it rather than owning it
     */
    public record Entry(UUID id, ResourceKey<Level> dimension, BlockPos pos, CarType type, int color, String plate, String name, boolean rented) {
        private static final StreamCodec<RegistryFriendlyByteBuf, ResourceKey<Level>> DIMENSION = ResourceKey.streamCodec(Registries.DIMENSION).cast();

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.of((buf, entry) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, entry.id());
            DIMENSION.encode(buf, entry.dimension());
            BlockPos.STREAM_CODEC.encode(buf, entry.pos());
            buf.writeVarInt(entry.type().ordinal());
            buf.writeInt(entry.color());
            buf.writeUtf(entry.plate(), 16);
            buf.writeUtf(entry.name(), 64);
            buf.writeBoolean(entry.rented());
        }, buf -> new Entry(UUIDUtil.STREAM_CODEC.decode(buf), DIMENSION.decode(buf), BlockPos.STREAM_CODEC.decode(buf),
                CarType.values()[Mth.clamp(buf.readVarInt(), 0, CarType.values().length - 1)], buf.readInt(), buf.readUtf(16), buf.readUtf(64),
                buf.readBoolean()));
    }

    public static final Type<GpsCarsPayload> TYPE = new Type<>(ReikoCars.id("gps_cars"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GpsCarsPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), GpsCarsPayload::cars,
            GpsCarsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
