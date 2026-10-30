package com.reiko.cars.road;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * What's painted on a block of asphalt. Each is drawn for traffic heading the block's {@code facing} way: lines run
 * along it, arrows point along it, a stop line lies across its far edge, and an edge line runs down its left side.
 */
public enum RoadMarking implements StringRepresentable {
    NONE("none"),
    CENTER_DASHED("center_dashed"),
    CENTER_SOLID("center_solid"),
    LANE_DASHED("lane_dashed"),
    EDGE("edge"),
    STOP_LINE("stop_line"),
    CROSSWALK("crosswalk"),
    ARROW_STRAIGHT("arrow_straight"),
    ARROW_LEFT("arrow_left"),
    ARROW_RIGHT("arrow_right");

    public static final Codec<RoadMarking> CODEC = StringRepresentable.fromEnum(RoadMarking::values);
    public static final StreamCodec<ByteBuf, RoadMarking> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);

    private final String name;

    RoadMarking(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public Component displayName() {
        return Component.translatable("road.reikocars.marking." + name);
    }

    /** The next marking a can of Road Paint offers ({@code step} 1) or the one before ({@code step} -1), skipping NONE. */
    public RoadMarking cycle(int step) {
        int n = values().length - 1;
        return values()[1 + Math.floorMod(ordinal() - 1 + step, n)];
    }
}
