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
 * The diagonal lines cross the block from corner to corner instead.
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
    ARROW_RIGHT("arrow_right"),
    // Lines across the block from corner to corner, for roads at 45 degrees: facing north (or south) the line runs
    // south-west to north-east, facing east (or west) north-west to south-east. A road at 45 degrees gets one of these
    // in each block along the line, so the line runs straight through them.
    CENTER_DASHED_DIAGONAL("center_dashed_diagonal"),
    CENTER_SOLID_DIAGONAL("center_solid_diagonal"),
    LANE_DASHED_DIAGONAL("lane_dashed_diagonal"),
    EDGE_DIAGONAL("edge_diagonal");

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

    /** The same line from corner to corner, for a road at 45 degrees; itself if it has no diagonal. */
    public RoadMarking diagonal() {
        return switch (this) {
            case CENTER_DASHED -> CENTER_DASHED_DIAGONAL;
            case CENTER_SOLID -> CENTER_SOLID_DIAGONAL;
            case LANE_DASHED -> LANE_DASHED_DIAGONAL;
            case EDGE -> EDGE_DIAGONAL;
            default -> this;
        };
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
