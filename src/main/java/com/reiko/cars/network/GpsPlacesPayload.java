package com.reiko.cars.network;

import com.reiko.cars.ReikoCars;
import com.reiko.cars.gps.PlaceKind;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;

/** Server to client: the places near you in this dimension, nearest first, for the GPS. */
public record GpsPlacesPayload(List<Entry> places) implements CustomPacketPayload {
    /** The most places sent at once. */
    public static final int MAX = 120;

    /**
     * A place: a pump, or a gas station's worth of them, and so on.
     *
     * @param detail its prices, or "closed"; empty if there's nothing to say
     * @param open   whether it can serve you (a station with fuel, a car wash that's open)
     */
    public record Entry(PlaceKind kind, BlockPos pos, String owner, Component detail, boolean open) {
        private static final StreamCodec<RegistryFriendlyByteBuf, PlaceKind> KIND = ByteBufCodecs.VAR_INT.map(
                i -> PlaceKind.values()[Mth.clamp(i, 0, PlaceKind.values().length - 1)], PlaceKind::ordinal).cast();
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                KIND, Entry::kind,
                BlockPos.STREAM_CODEC, Entry::pos,
                ByteBufCodecs.stringUtf8(64), Entry::owner,
                ComponentSerialization.STREAM_CODEC, Entry::detail,
                ByteBufCodecs.BOOL, Entry::open,
                Entry::new);
    }

    public static final Type<GpsPlacesPayload> TYPE = new Type<>(ReikoCars.id("gps_places"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GpsPlacesPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(MAX)), GpsPlacesPayload::places,
            GpsPlacesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
